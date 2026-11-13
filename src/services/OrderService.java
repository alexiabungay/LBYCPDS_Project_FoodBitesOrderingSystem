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
        if (customer.getCart().isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty.");
        }
        return billCalculator.compute(customer.getCart(), orderType, promoCode);
    }

    public features.Order checkout(Customer customer, DeliveryDetails details, PaymentMethod paymentMethod,
                                   String promoCode, Boolean forcedPaymentResult) {
        Cart cart = customer.getCart();
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty.");
        }

        Restaurant restaurant = store.getRestaurants().search(cart.getRestaurantId());
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
        features.Order order = new features.Order(store.newOrderId(), customer.getId(), restaurant.getId(),
                cart.getItems(), details, bill);
        Payment payment = paymentService.process(bill.getTotal(), paymentMethod, forcedPaymentResult);
        order.setPayment(payment);
        store.getOrders().insert(order.getId(), order);
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
        store.saveRestaurant(restaurant);
        return order;
    }

    public int getQueuePosition(features.Order order) {
        Restaurant restaurant = store.getRestaurants().search(order.getRestaurantId());
        if (restaurant == null) {
            return -1;
        }
        return restaurant.getKitchenQueue().positionOf(order);
    }

    public features.Order getOrder(int orderId) {
        features.Order order = store.getOrders().search(orderId);
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
            User rider = store.getUsersById().search(order.getRiderId());
            String riderName = "-";
            if (rider != null) {
                riderName = rider.getName();
            }
            trackingText.append("  Rider: ").append(riderName).append('\n');
        }

        trackingText.append("  History:\n");
        for (StatusDelivery statusChange : order.getHistory()) {
            trackingText.append("    ").append(statusChange).append('\n');
        }
        return trackingText.toString();
    }

    private String statusMessage(features.Order order) {
        switch (order.getStatus()) {
            case CONFIRMED:
                return "Order confirmed. Waiting in the kitchen queue.";
            case PREPARING:
                return "The kitchen has started preparing your order.";
            case READY:
                if (order.getDetails().getType() == OrderType.DELIVERY) {
                    return "Order is ready and waiting for a rider.";
                }
                return "Order is ready for pickup.";
            case OUT_FOR_DELIVERY:
                return "Your order is out for delivery.";
            case DELIVERED:
                return "Delivered.";
            case COMPLETED:
                return "Completed.";
            case CANCELLED:
                return "Cancelled.";
            case FAILED:
                return "Failed (payment or delivery did not succeed).";
            case REFUNDED:
                return "Refunded.";
            default:
                return "Waiting for payment.";
        }
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
        if (frontOrder.getDetails().getType() == OrderType.DELIVERY) {
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
        if (order.getDetails().getType() == OrderType.DELIVERY) {
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
        for (Restaurant restaurant : store.getRestaurants().inOrder()) {
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
        Restaurant restaurant = store.getRestaurants().search(nextOrder.getRestaurantId());
        restaurant.getDispatchQueue().remove(nextOrder);

        nextOrder.setRiderId(rider.getId());
        nextOrder.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        rider.setAvailable(false);
        return nextOrder;
    }

    public features.Order confirmDelivery(DeliveryRider rider, int orderId, boolean arrived) {
        features.Order order = getOrder(orderId);
        boolean isOutWithThisRider = order.getRiderId() == rider.getId()
                && order.getStatus() == OrderStatus.OUT_FOR_DELIVERY;
        if (!isOutWithThisRider) {
            throw new IllegalStateException("That order is not out for delivery with you.");
        }

        Restaurant restaurant = store.getRestaurants().search(order.getRestaurantId());
        if (arrived) {
            order.setStatus(OrderStatus.DELIVERED);
            finalizeSale(order, restaurant);
            rider.getDeliveredOrderIds().add(order.getId());
        } else {
            order.setStatus(OrderStatus.FAILED);
            restoreStock(order);
            if (order.getPayment() != null) {
                order.getPayment().markRefunded();
            }
            restaurant.customerLeft(false);
            restaurant.addSalesRecord(new SalesRecord(order.getId(), RecordType.CANCELLATION, LocalDate.now(),
                    order.getBill().getTotal(), order.getItems()));
            store.saveRestaurant(restaurant);
        }

        rider.setAvailable(true);
        return order;
    }

    public features.Order cancelOrder(User actor, int orderId) {
        features.Order order = getOrder(orderId);
        Restaurant restaurant = store.getRestaurants().search(order.getRestaurantId());

        if (actor.getRole() == UserRole.CUSTOMER) {
            if (order.getCustomerId() != actor.getId()) {
                throw new SecurityException("That is not your order.");
            }
            if (order.getStatus() != OrderStatus.CONFIRMED) {
                throw new IllegalStateException("The kitchen already started this order. Please contact the restaurant.");
            }
        } else {
            requireStaff(actor, restaurant);
            OrderStatus orderStatus = order.getStatus();
            boolean canStillBeCancelled = orderStatus == OrderStatus.CONFIRMED
                    || orderStatus == OrderStatus.PREPARING
                    || orderStatus == OrderStatus.READY;
            if (!canStillBeCancelled) {
                throw new IllegalStateException("This order can no longer be cancelled.");
            }
        }

        restaurant.getKitchenQueue().remove(order);
        restaurant.getDispatchQueue().remove(order);
        restoreStock(order);
        if (order.getPayment() != null) {
            order.getPayment().markRefunded();
        }
        order.setStatus(OrderStatus.CANCELLED);
        restaurant.customerLeft(false);
        restaurant.addSalesRecord(new SalesRecord(order.getId(), RecordType.CANCELLATION, LocalDate.now(),
                order.getBill().getTotal(), order.getItems()));
        store.saveRestaurant(restaurant);
        return order;
    }

    public features.Order refundOrder(User actor, int orderId) {
        features.Order order = getOrder(orderId);
        Restaurant restaurant = store.getRestaurants().search(order.getRestaurantId());

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
        order.getPayment().markRefunded();
        order.setStatus(OrderStatus.REFUNDED);
        restaurant.addSalesRecord(new SalesRecord(order.getId(), RecordType.REFUND, LocalDate.now(),
                order.getBill().getTotal(), order.getItems()));
        return order;
    }

    public List<features.Order> listOrders(User actor, int restaurantId, boolean activeOnly) {
        Restaurant restaurant = store.getRestaurants().search(restaurantId);
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant not found.");
        }
        requireStaff(actor, restaurant);

        List<features.Order> matchingOrders = new ArrayList<>();
        for (features.Order order : store.getOrders().inOrder()) {
            if (order.getRestaurantId() != restaurantId) {
                continue;
            }

            OrderStatus orderStatus = order.getStatus();
            boolean isActive = orderStatus == OrderStatus.CONFIRMED
                    || orderStatus == OrderStatus.PREPARING
                    || orderStatus == OrderStatus.READY
                    || orderStatus == OrderStatus.OUT_FOR_DELIVERY;
            if (activeOnly == isActive) {
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
        restaurant.addSalesRecord(new SalesRecord(order.getId(), RecordType.SALE, LocalDate.now(),
                order.getBill().getTotal(), order.getItems()));
    }

    private void restoreStock(features.Order order) {
        for (OrderItem orderItem : order.getItems()) {
            orderItem.getItem().restock(orderItem.getQuantity());
        }
    }

    private Restaurant restaurantOf(Employee employee) {
        Restaurant restaurant = store.getRestaurants().search(employee.getRestaurantId());
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