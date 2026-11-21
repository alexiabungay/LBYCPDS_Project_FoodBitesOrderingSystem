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
        if (actor.getRole() == UserRole.EMPLOYEE || !OrderService.canManage(actor, restaurant)) {
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
        if (!OrderService.canManage(actor, restaurant)) {
            throw new SecurityException("No access to this restaurant.");
        }

        StringBuilder report = new StringBuilder("INVENTORY - " + restaurant.getName() + "\n");
        List<MenuItem> menuItems = restaurant.listAlphabetical();
        if (menuItems.isEmpty()) {
            report.append("No items to show.\n");
        } else {
            report.append(DisplayMenu.forInventory(menuItems, LOW_STOCK_LIMIT)).append('\n');
            report.append(menuItems.size()).append(" item(s)\n");
        }
        return report.toString();
    }

    public String foodplacesSummary(User admin) {
        if (admin == null || admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new SecurityException("Administrators only.");
        }

        int orderCount = 0;
        int cancellationCount = 0;
        int refundCount = 0;
        double grossSales = 0;
        double refundTotal = 0;
        Restaurant topRestaurant = null;

        List<Restaurant> allRestaurants = store.getRestaurants().inOrder();
        for (Restaurant restaurant : allRestaurants) {
            for (SalesRecord salesRecord : restaurant.getSalesRecords()) {
                switch (salesRecord.getType()) {
                    case SALE:
                        orderCount++;
                        grossSales += salesRecord.getAmount();
                        break;
                    case CANCELLATION:
                        cancellationCount++;
                        break;
                    default:
                        refundCount++;
                        refundTotal += salesRecord.getAmount();
                }
            }
            if (topRestaurant == null || restaurant.getTotalSales() > topRestaurant.getTotalSales()) {
                topRestaurant = restaurant;
            }
        }

        String topName = "(no sales yet)";
        if (topRestaurant != null && topRestaurant.getTotalSales() > 0) {
            topName = topRestaurant.getName() + String.format(" (PHP %.2f)", topRestaurant.getTotalSales());
        }

        return "FOODPLACES SUMMARY" + System.lineSeparator()
                + DisplayMenu.forSalesSummary(new String[][]{
                {"Restaurants", String.valueOf(allRestaurants.size())},
                {"Orders completed", String.valueOf(orderCount)},
                {"Gross revenue", String.format("PHP %.2f", grossSales)},
                {"Cancellations", String.valueOf(cancellationCount)},
                {"Refunds issued", String.valueOf(refundCount)},
                {"Refund total", String.format("PHP %.2f", refundTotal)},
                {"Net revenue", String.format("PHP %.2f", grossSales - refundTotal)},
                {"Top restaurant", topName}
        });
    }

    public List<Restaurant> rankRestaurants(User actor) {
        if (actor.getRole() != UserRole.ADMINISTRATOR) {
            throw new SecurityException("Administrators only.");
        }
        return HeapSort.sortDescending(store.getRestaurants().inOrder(),
                Comparator.comparingDouble(Restaurant::getTotalSales));
    }
}