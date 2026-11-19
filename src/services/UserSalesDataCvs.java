package services;

import data.structure.AVLTree;
import features.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Saves user and sales data to CSV files. Every save is an append, so nothing is overwritten.
 *
 *   users.csv        - every registered account (loaded again the next time the program runs)
 *   orders.csv       - one row every time an order is placed or its status changes
 *                      (dine in / takeout / delivery, completed, cancelled, refunded, failed, ...)
 *   order_items.csv  - the food ordered in each order
 *   sales.csv        - the food sold, cancelled or refunded (loaded again for the sales report)
 *   receipts/        - the e-receipt of each confirmed order as a text file
 */
public class UserSalesDataCvs {
    private static final String USER_HEADER =
            "user_id,role,name,email,phone,password_hash,restaurant_id,registered_at";
    private static final String ORDER_HEADER =
            "logged_at,order_id,restaurant_id,customer_id,order_type,status,payment_method,payment_status,"
                    + "payment_reference,subtotal,discount,tax,service_charge,delivery_fee,total,"
                    + "address,preferred_time,special_instructions,rider_id";
    private static final String ORDER_ITEM_HEADER =
            "order_id,restaurant_id,item_name,details,quantity,unit_price,line_total";
    private static final String SALES_HEADER =
            "date,order_id,restaurant_id,record_type,item_name,details,quantity,unit_price,line_total,record_amount";

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path folder;
    private final Path usersFile;
    private final Path ordersFile;
    private final Path orderItemsFile;
    private final Path salesFile;
    private final Path receiptsFolder;

    public UserSalesDataCvs(String folderName) {
        this.folder = Paths.get(folderName);
        this.usersFile = folder.resolve("users.csv");
        this.ordersFile = folder.resolve("orders.csv");
        this.orderItemsFile = folder.resolve("order_items.csv");
        this.salesFile = folder.resolve("sales.csv");
        this.receiptsFolder = folder.resolve("receipts");
    }

    // ---------------------------------------------------------------- writing

    private String now() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    private String money(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private void append(Path file, String header, List<String> fields) {
        try {
            Files.createDirectories(folder);
            boolean needsHeader = !Files.exists(file) || Files.size(file) == 0;

            StringBuilder text = new StringBuilder();
            if (needsHeader) {
                text.append(header).append(System.lineSeparator());
            }
            text.append(Csv.join(fields)).append(System.lineSeparator());

            Files.write(file, text.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            System.err.println("! Could not save to " + file.getFileName() + ": " + exception.getMessage());
        }
    }

    public void appendUser(User user) {
        int restaurantId = -1;
        if (user instanceof Employee) {
            restaurantId = ((Employee) user).getRestaurantId();
        }
        append(usersFile, USER_HEADER, Arrays.asList(
                String.valueOf(user.getId()),
                user.getRole().name(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getPasswordHash(),
                String.valueOf(restaurantId),
                now()));
    }

    // One row per call: save it again whenever the order changes status so the file keeps the full history.
    public void appendOrder(Order order) {
        Bill bill = order.getBill();
        Payment payment = order.getPayment();
        DeliveryDetails details = order.getDetails();

        String paymentMethod = "";
        String paymentStatus = "";
        String paymentReference = "";
        if (payment != null) {
            paymentMethod = payment.getMethod().name();
            paymentStatus = payment.getStatus().name();
            paymentReference = payment.getReferenceNumber();
        }

        append(ordersFile, ORDER_HEADER, Arrays.asList(
                now(),
                String.valueOf(order.getId()),
                String.valueOf(order.getRestaurantId()),
                String.valueOf(order.getCustomerId()),
                details.getType().name(),
                order.getStatus().name(),
                paymentMethod,
                paymentStatus,
                paymentReference,
                money(bill.getSubtotal()),
                money(bill.getDiscount()),
                money(bill.getTax()),
                money(bill.getServiceCharge()),
                money(bill.getDeliveryFee()),
                money(bill.getTotal()),
                details.getAddress(),
                details.getPreferredTime(),
                details.getSpecialInstructions(),
                String.valueOf(order.getRiderId())));
    }

    // The food of an order. Call once when the order is placed.
    public void appendOrderItems(Order order) {
        for (OrderItem orderItem : order.getItems()) {
            append(orderItemsFile, ORDER_ITEM_HEADER, Arrays.asList(
                    String.valueOf(order.getId()),
                    String.valueOf(order.getRestaurantId()),
                    orderItem.getName(),
                    orderItem.getDetails(),
                    String.valueOf(orderItem.getQuantity()),
                    money(orderItem.getUnitPrice()),
                    money(orderItem.getLineTotal())));
        }
    }

    // The food sold (or cancelled / refunded), used by the sales report.
    public void appendSalesRecord(int restaurantId, SalesRecord salesRecord) {
        for (OrderItem orderItem : salesRecord.getItems()) {
            append(salesFile, SALES_HEADER, Arrays.asList(
                    salesRecord.getDate().toString(),
                    String.valueOf(salesRecord.getOrderId()),
                    String.valueOf(restaurantId),
                    salesRecord.getType().name(),
                    orderItem.getName(),
                    orderItem.getDetails(),
                    String.valueOf(orderItem.getQuantity()),
                    money(orderItem.getUnitPrice()),
                    money(orderItem.getLineTotal()),
                    money(salesRecord.getAmount())));
        }
    }

    public void saveReceipt(int orderId, String receiptText) {
        try {
            Files.createDirectories(receiptsFolder);
            Files.write(receiptsFolder.resolve("receipt_" + orderId + ".txt"),
                    receiptText.getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            System.err.println("! Could not save receipt: " + exception.getMessage());
        }
    }

    // ---------------------------------------------------------------- reading

    private List<List<String>> readRows(Path file) {
        List<List<String>> rows = new ArrayList<>();
        if (!Files.exists(file)) {
            return rows;
        }

        try {
            boolean isFirstLine = true;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (isFirstLine) {
                    isFirstLine = false;     // header
                    continue;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                rows.add(Csv.parseLine(line));
            }
        } catch (IOException exception) {
            System.err.println("! Could not read " + file.getFileName() + ": " + exception.getMessage());
        }
        return rows;
    }

    public List<User> readUsers() {
        List<User> users = new ArrayList<>();
        for (List<String> row : readRows(usersFile)) {
            try {
                int id = Integer.parseInt(row.get(0));
                UserRole role = UserRole.valueOf(row.get(1));
                String name = row.get(2);
                String email = row.get(3);
                String phone = row.get(4);
                String passwordHash = row.get(5);
                int restaurantId = Integer.parseInt(row.get(6));

                // the password argument is only a placeholder, the saved hash replaces it
                String placeholder = "placeholder1";
                User user;
                switch (role) {
                    case CUSTOMER:
                        user = new Customer(id, name, email, phone, placeholder);
                        break;
                    case DELIVERY_RIDER:
                        user = new DeliveryRider(id, name, email, phone, placeholder);
                        break;
                    case BUSINESS_OWNER:
                        user = new BusinessOwner(id, name, email, phone, placeholder);
                        break;
                    case EMPLOYEE:
                        user = new Employee(id, name, email, phone, placeholder, restaurantId);
                        break;
                    default:
                        user = new Admin(id, name, email, phone, placeholder);
                }
                user.restorePasswordHash(passwordHash);
                users.add(user);
            } catch (RuntimeException exception) {
                System.err.println("! Skipped a saved account in users.csv: " + exception.getMessage());
            }
        }
        return users;
    }

    // Puts the saved sales back into the restaurants so the sales report and best sellers keep their history.
    public int loadSales(AVLTree<Integer, Restaurant> restaurants) {
        Map<String, List<List<String>>> rowsPerRecord = new LinkedHashMap<>();
        for (List<String> row : readRows(salesFile)) {
            if (row.size() < 10) {
                continue;
            }
            String key = row.get(0) + "|" + row.get(1) + "|" + row.get(2) + "|" + row.get(3);
            rowsPerRecord.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }

        int loadedCount = 0;
        for (List<List<String>> rows : rowsPerRecord.values()) {
            try {
                List<String> first = rows.get(0);
                LocalDate date = LocalDate.parse(first.get(0));
                int orderId = Integer.parseInt(first.get(1));
                Restaurant restaurant = restaurants.search(Integer.parseInt(first.get(2)));
                if (restaurant == null) {
                    continue;
                }
                RecordType type = RecordType.valueOf(first.get(3));
                double recordAmount = Double.parseDouble(first.get(9));

                List<OrderItem> items = new ArrayList<>();
                for (List<String> row : rows) {
                    MenuItem menuItem = restaurant.findByName(row.get(4));
                    items.add(new OrderItem(menuItem, row.get(4), Double.parseDouble(row.get(7)),
                            Integer.parseInt(row.get(6)), row.get(5)));
                }
                restaurant.addSalesRecord(new SalesRecord(orderId, type, date, recordAmount, items));

                if (type == RecordType.SALE) {
                    restaurant.addRevenue(recordAmount);
                    for (OrderItem item : items) {
                        if (item.getItem() != null) {
                            item.getItem().recordSale(item.getQuantity(), item.getLineTotal());
                        }
                    }
                } else if (type == RecordType.REFUND) {
                    restaurant.subtractRevenue(recordAmount);
                    for (OrderItem item : items) {
                        if (item.getItem() != null) {
                            item.getItem().reverseSale(item.getQuantity(), item.getLineTotal());
                        }
                    }
                }
                loadedCount++;
            } catch (RuntimeException exception) {
                System.err.println("! Skipped a saved sales record in sales.csv: " + exception.getMessage());
            }
        }
        return loadedCount;
    }

    // Highest order number saved so far, so new orders do not reuse old numbers.
    public int highestOrderId() {
        int highest = 0;
        for (List<String> row : readRows(ordersFile)) {
            try {
                highest = Math.max(highest, Integer.parseInt(row.get(1)));
            } catch (RuntimeException exception) {
                // ignore a damaged row
            }
        }
        return highest;
    }
}
