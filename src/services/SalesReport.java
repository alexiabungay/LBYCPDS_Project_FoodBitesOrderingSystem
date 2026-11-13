package services;

import data.structure.HeapSort;
import features.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalesReport {
    private static final int LOW_STOCK_LIMIT = 5;

    private final DataStoring store;

    public SalesReport(DataStoring store) {
        this.store = store;
    }

    public features.SalesReportFeature generate(User actor, int restaurantId, ReportType period, LocalDate referenceDate) {
        Restaurant restaurant = store.getRestaurants().search(restaurantId);
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant not found.");
        }
        if (actor.getRole() == UserRole.EMPLOYEE || !Order.canManage(actor, restaurant)) {
            throw new SecurityException("Only the owner or an administrator can view sales reports.");
        }

        LocalDate startDate;
        LocalDate endDate;
        switch (period) {
            case DAILY:
                startDate = referenceDate;
                endDate = referenceDate;
                break;
            case WEEKLY:
                startDate = referenceDate.minusDays(6);
                endDate = referenceDate;
                break;
            default:
                startDate = referenceDate.withDayOfMonth(1);
                endDate = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth());
        }

        int orderCount = 0;
        int cancellationCount = 0;
        double grossSales = 0;
        double refundTotal = 0;
        Map<String, ItemSales> salesPerItem = new HashMap<>();

        for (SalesRecord salesRecord : restaurant.getSalesRecords()) {
            boolean isOutsidePeriod = salesRecord.getDate().isBefore(startDate) || salesRecord.getDate().isAfter(endDate);
            if (isOutsidePeriod) {
                continue;
            }

            switch (salesRecord.getType()) {
                case SALE:
                    orderCount++;
                    grossSales += salesRecord.getAmount();
                    addItems(salesPerItem, salesRecord, 1);
                    break;
                case CANCELLATION:
                    cancellationCount++;
                    break;
                default:
                    refundTotal += salesRecord.getAmount();
                    addItems(salesPerItem, salesRecord, -1);
            }
        }

        List<ItemSales> soldItems = new ArrayList<>();
        for (ItemSales itemSales : salesPerItem.values()) {
            if (itemSales.getUnits() > 0) {
                soldItems.add(itemSales);
            }
        }

        List<ItemSales> rankedItems = HeapSort.sortDescending(soldItems,
                Comparator.comparingInt(ItemSales::getUnits).thenComparingDouble(ItemSales::getRevenue));
        return new features.SalesReportFeature(restaurant.getName(), period, startDate, endDate,
                orderCount, grossSales, cancellationCount, refundTotal, rankedItems);
    }

    private void addItems(Map<String, ItemSales> salesPerItem, SalesRecord salesRecord, int multiplier) {
        for (OrderItem orderItem : salesRecord.getItems()) {
            ItemSales itemSales = salesPerItem.computeIfAbsent(orderItem.getName(), ItemSales::new);
            itemSales.add(multiplier * orderItem.getQuantity(), multiplier * orderItem.getLineTotal());
        }
    }

    public List<MenuItem> bestSellers(Restaurant restaurant) {
        return restaurant.listByPopularity();
    }

    public String inventoryReport(User actor, int restaurantId) {
        Restaurant restaurant = store.getRestaurants().search(restaurantId);
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant not found.");
        }
        if (!Order.canManage(actor, restaurant)) {
            throw new SecurityException("No access to this restaurant.");
        }

        StringBuilder report = new StringBuilder("INVENTORY - " + restaurant.getName() + "\n");
        for (MenuItem menuItem : restaurant.listAlphabetical()) {
            String availability = "Unavailable";
            if (menuItem.isAvailable()) {
                availability = "Available";
            }

            String lowStockWarning = "";
            if (menuItem.getStock() <= LOW_STOCK_LIMIT) {
                lowStockWarning = "  LOW STOCK";
            }

            report.append(String.format("  %-22s stock %-4d %-12s%s%n",
                    menuItem.getName(), menuItem.getStock(), availability, lowStockWarning));
        }
        return report.toString();
    }

    public List<Restaurant> rankRestaurants(User actor) {
        if (actor.getRole() != UserRole.ADMINISTRATOR) {
            throw new SecurityException("Administrators only.");
        }
        return HeapSort.sortDescending(store.getRestaurants().inOrder(),
                Comparator.comparingDouble(Restaurant::getTotalSales));
    }
}