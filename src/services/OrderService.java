package services;

import features.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OrderService {
    private final DataStoring store;
    private final BillCalculator billCalculator;
    private final PaymentService paymentService;

    public OrderService(DataStoring store, BillCalculator billCalculator, PaymentService paymentService) {
        this.store = store;
        this.billCalculator = billCalculator;
        this.paymentService = paymentService;
    }

    public Bill previewBill(Customer customer, OrderType orderType, String promoCode) {
        return billCalculator.compute(requireCart(customer), orderType, promoCode);
    }

    public features.Order checkout(Customer customer, DeliveryDetails details, PaymentMethod paymentMethod,
                                   String promoCode, Boolean forcedPaymentResult) {
        Cart cart = requireCart(customer);

        Restaurant restaurant = findRestaurant(cart.getRestaurantId());
        if (restaurant == null) {
            throw new IllegalArgumentException("That restaurant is no longer available.");
        }
        if (!restaurant.supports(details.getType())) {
            throw new IllegalArgumentException(restaurant.getName() + " does not support " + details.getType() + " orders.");
        }
        if (details.getType() == OrderType.DELIVERY && !Validator.isNotBlank(details.getAddress())) {
            throw new IllegalArgumentException("A delivery address is required.");
        }

        List<String> availabilityProblems = cart.validateAvailability();
        if (!availabilityProblems.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", availabilityProblems));
        }

        Bill bill = billCalculator.compute(cart, details.getType(), promoCode);
        features.Order order = new features.Order(store.generateNextOrderId(), customer.getId(), restaurant.getId(),
                cart.getItems(), details, bill);
        Payment payment = paymentService.process(bill.getTotal(), paymentMethod, forcedPaymentResult);
        order.setPayment(payment);
        store.addOrder(order);
        customer.getOrderIds().add(order.getId());

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            order.setStatus(OrderStatus.FAILED);
            return order;
        }

        for (OrderItem orderItem : order.getItems()) {
            orderItem.getItem().deductStock(orderItem.getQuantity());
        }
        order.setStatus(OrderStatus.CONFIRMED);
        restaurant.getKitchenQueue().enqueue(order);
        restaurant.customerArrived();
        cart.clear();
        return order;
    }

    public int getQueuePosition(features.Order order) {
        Restaurant restaurant = findRestaurant(order.getRestaurantId());
        return restaurant == null ? -1 : restaurant.getKitchenQueue().positionOf(order);
    }

    public features.Order getOrder(int orderId) {
        features.Order order = findOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found.");
        }
        return order;
    }

    public List<features.Order> getOrdersOf(Customer customer) {
        List<features.Order> customerOrders = new ArrayList<>();
        for (int orderId : customer.getOrderIds()) {
            customerOrders.add(getOrder(orderId));
        }
        return customerOrders;
    }

    public String trackOrder(Customer customer, int orderId) {
        features.Order order = getOrder(orderId);
        if (order.getCustomerId() != customer.getId()) {
            throw new SecurityException("That is not your order.");
        }

        StringBuilder trackingText = new StringBuilder();
        trackingText.append(order).append('\n');
        trackingText.append("  Status: ").append(statusMessage(order)).append('\n');

        int queuePosition = getQueuePosition(order);
        if (queuePosition > 0) {
            trackingText.append("  Queue position: ").append(queuePosition).append('\n');
        }

        if (order.getRiderId() != -1) {
            User rider = findUser(order.getRiderId());
            trackingText.append("  Rider: ").append(rider == null ? "-" : rider.getName()).append('\n');
        }

        trackingText.append("  History:\n");
        for (StatusDelivery statusChange : order.getHistory()) {
            trackingText.append("    ").append(statusChange).append('\n');
        }
        return trackingText.toString();
    }

    private String statusMessage(features.Order order) {
        return switch (order.getStatus()) {
            case CONFIRMED -> "Order confirmed. Waiting in the kitchen queue.";
            case PREPARING -> "The kitchen has started preparing your order.";
            case READY -> isDelivery(order) ? "Order is ready and waiting for a rider." : "Order is ready for pickup.";
            case OUT_FOR_DELIVERY -> "Your order is out for delivery.";
            case DELIVERED -> "Delivered.";
            case COMPLETED -> "Completed.";
            case CANCELLED -> "Cancelled.";
            case FAILED -> "Failed (payment or delivery did not succeed).";
            case REFUNDED -> "Refunded.";
            default -> "Waiting for payment.";
        };
    }

    public List<features.Order> viewKitchenQueue(Employee employee) {
        return restaurantOf(employee).getKitchenQueue().toList();
    }

    public features.Order startPreparing(Employee employee) {
        features.Order frontOrder = restaurantOf(employee).getKitchenQueue().peek();
        if (frontOrder == null) {
            throw new IllegalStateException("The kitchen queue is empty.");
        }
        if (frontOrder.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order #" + frontOrder.getId() + " is already being prepared. Mark it ready first.");
        }
        frontOrder.setStatus(OrderStatus.PREPARING);
        return frontOrder;
    }

    public features.Order markReady(Employee employee) {
        Restaurant restaurant = restaurantOf(employee);
        features.Order frontOrder = restaurant.getKitchenQueue().peek();
        if (frontOrder == null) {
            throw new IllegalStateException("The kitchen queue is empty.");
        }
        if (frontOrder.getStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("Start preparing order #" + frontOrder.getId() + " first.");
        }

        restaurant.getKitchenQueue().dequeue();
        frontOrder.setStatus(OrderStatus.READY);
        if (isDelivery(frontOrder)) {
            restaurant.getDispatchQueue().enqueue(frontOrder);
        }
        return frontOrder;
    }

    public features.Order completeOrder(Employee employee, int orderId) {
        Restaurant restaurant = restaurantOf(employee);
        features.Order order = getOrder(orderId);

        if (order.getRestaurantId() != restaurant.getId()) {
            throw new SecurityException("That order belongs to another restaurant.");
        }
        if (isDelivery(order)) {
            throw new IllegalStateException("Delivery orders are completed by the rider.");
        }
        if (order.getStatus() != OrderStatus.READY) {
            throw new IllegalStateException("Order is not ready yet.");
        }

        order.setStatus(OrderStatus.COMPLETED);
        finalizeSale(order, restaurant);
        return order;
    }

    public List<features.Order> pendingDeliveries() {
        List<features.Order> pendingOrders = new ArrayList<>();
        for (Restaurant restaurant : store.getRestaurants()) {
            pendingOrders.addAll(restaurant.getDispatchQueue().toList());
        }
        pendingOrders.sort(Comparator.comparingInt(features.Order::getId));
        return pendingOrders;
    }

    public features.Order acceptNextDelivery(DeliveryRider rider) {
        if (!rider.isAvailable()) {
            throw new IllegalStateException("Finish your current delivery first.");
        }

        List<features.Order> pendingOrders = pendingDeliveries();
        if (pendingOrders.isEmpty()) {
            throw new IllegalStateException("No pending deliveries.");
        }

        features.Order nextOrder = pendingOrders.get(0);
        Restaurant restaurant = restaurantOf(nextOrder);
        restaurant.getDispatchQueue().remove(nextOrder);

        nextOrder.setRiderId(rider.getId());
        nextOrder.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        rider.setAvailable(false);
        return nextOrder;
    }

    public features.Order confirmDelivery(DeliveryRider rider, int orderId, boolean arrived) {
        features.Order order = getOrder(orderId);
        if (order.getRiderId() != rider.getId() || order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("That order is not out for delivery with you.");
        }

        Restaurant restaurant = restaurantOf(order);
        if (arrived) {
            order.setStatus(OrderStatus.DELIVERED);
            finalizeSale(order, restaurant);
            rider.getDeliveredOrderIds().add(order.getId());
        } else {
            order.setStatus(OrderStatus.FAILED);
            rollback(order, restaurant);
        }

        rider.setAvailable(true);
        return order;
    }

    public features.Order cancelOrder(User actor, int orderId) {
        features.Order order = getOrder(orderId);
        Restaurant restaurant = restaurantOf(order);

        if (actor.getRole() == UserRole.CUSTOMER) {
            if (order.getCustomerId() != actor.getId()) {
                throw new SecurityException("That is not your order.");
            }
            if (order.getStatus() != OrderStatus.CONFIRMED) {
                throw new IllegalStateException("The kitchen already started this order. Please contact the restaurant.");
            }
        } else {
            requireStaff(actor, restaurant);
            if (!isCancellable(order.getStatus())) {
                throw new IllegalStateException("This order can no longer be cancelled.");
            }
        }

        restaurant.getKitchenQueue().remove(order);
        restaurant.getDispatchQueue().remove(order);
        order.setStatus(OrderStatus.CANCELLED);
        rollback(order, restaurant);
        return order;
    }

    public features.Order refundOrder(User actor, int orderId) {
        features.Order order = getOrder(orderId);
        Restaurant restaurant = restaurantOf(order);

        requireStaff(actor, restaurant);
        if (actor.getRole() == UserRole.EMPLOYEE) {
            throw new SecurityException("Only owners and administrators can issue refunds.");
        }
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Only completed or delivered orders can be refunded.");
        }

        for (OrderItem orderItem : order.getItems()) {
            orderItem.getItem().reverseSale(orderItem.getQuantity(), orderItem.getLineTotal());
        }
        restaurant.subtractRevenue(order.getBill().getTotal());
        refundPayment(order);
        order.setStatus(OrderStatus.REFUNDED);
        addSalesRecord(order, restaurant, RecordType.REFUND);
        return order;
    }

    public List<features.Order> listOrders(User actor, int restaurantId, boolean activeOnly) {
        Restaurant restaurant = findRestaurant(restaurantId);
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant not found.");
        }
        requireStaff(actor, restaurant);

        List<features.Order> matchingOrders = new ArrayList<>();
        for (features.Order order : store.getOrders()) {
            if (order.getRestaurantId() == restaurantId && activeOnly == isActive(order.getStatus())) {
                matchingOrders.add(order);
            }
        }
        return matchingOrders;
    }

    private void finalizeSale(features.Order order, Restaurant restaurant) {
        for (OrderItem orderItem : order.getItems()) {
            orderItem.getItem().recordSale(orderItem.getQuantity(), orderItem.getLineTotal());
        }
        restaurant.addRevenue(order.getBill().getTotal());
        restaurant.customerLeft(true);
        addSalesRecord(order, restaurant, RecordType.SALE);
    }

    private void rollback(features.Order order, Restaurant restaurant) {
        restoreStock(order);
        refundPayment(order);
        restaurant.customerLeft(false);
        addSalesRecord(order, restaurant, RecordType.CANCELLATION);
    }

    private void restoreStock(features.Order order) {
        for (OrderItem orderItem : order.getItems()) {
            orderItem.getItem().restock(orderItem.getQuantity());
        }
    }

    private void refundPayment(features.Order order) {
        if (order.getPayment() != null) {
            order.getPayment().markRefunded();
        }
    }

    private void addSalesRecord(features.Order order, Restaurant restaurant, RecordType recordType) {
        restaurant.addSalesRecord(new SalesRecord(order.getId(), recordType, LocalDate.now(),
                order.getBill().getTotal(), order.getItems()));
    }

    private static Cart requireCart(Customer customer) {
        Cart cart = customer.getCart();
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty.");
        }
        return cart;
    }

    private static boolean isDelivery(features.Order order) {
        return order.getDetails().getType() == OrderType.DELIVERY;
    }

    private static boolean isActive(OrderStatus status) {
        return status == OrderStatus.CONFIRMED
                || status == OrderStatus.PREPARING
                || status == OrderStatus.READY
                || status == OrderStatus.OUT_FOR_DELIVERY;
    }

    private static boolean isCancellable(OrderStatus status) {
        return isActive(status) && status != OrderStatus.OUT_FOR_DELIVERY;
    }

    private Restaurant findRestaurant(int restaurantId) {
        for (Restaurant restaurant : store.getRestaurants()) {
            if (restaurant.getId() == restaurantId) {
                return restaurant;
            }
        }
        return null;
    }

    private features.Order findOrder(int orderId) {
        for (features.Order order : store.getOrders()) {
            if (order.getId() == orderId) {
                return order;
            }
        }
        return null;
    }

    private User findUser(int userId) {
        for (User user : store.getUsers()) {
            if (user.getId() == userId) {
                return user;
            }
        }
        return null;
    }

    private Restaurant restaurantOf(features.Order order) {
        return findRestaurant(order.getRestaurantId());
    }

    private Restaurant restaurantOf(Employee employee) {
        Restaurant restaurant = findRestaurant(employee.getRestaurantId());
        if (restaurant == null) {
            throw new IllegalStateException("Your restaurant no longer exists.");
        }
        return restaurant;
    }

    public static boolean canManage(User actor, Restaurant restaurant) {
        if (actor == null) {
            return false;
        }
        switch (actor.getRole()) {
            case ADMINISTRATOR:
                return true;
            case BUSINESS_OWNER:
                return restaurant.getOwnerId() == actor.getId();
            case EMPLOYEE:
                return ((Employee) actor).getRestaurantId() == restaurant.getId();
            default:
                return false;
        }
    }

    private void requireStaff(User actor, Restaurant restaurant) {
        if (!canManage(actor, restaurant)) {
            throw new SecurityException("You do not have access to this restaurant's orders.");
        }
    }
}
