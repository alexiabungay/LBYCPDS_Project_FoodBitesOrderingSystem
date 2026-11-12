package features;

import java.time.LocalDate;
import java.util.List;

public class SalesReportFeature {
    private final String restaurantName;
    private final ReportType period;
    private final LocalDate from;
    private final LocalDate to;
    private final int orderCount;
    private final double grossRevenue;
    private final int cancellations;
    private final double refunds;
    private final List<ItemSales> bestSellers;

    public SalesReportFeature(String restaurantName, ReportType period, LocalDate from, LocalDate to,
                       int orderCount, double grossRevenue, int cancellations, double refunds,
                       List<ItemSales> bestSellers) {
        this.restaurantName = restaurantName;
        this.period = period;
        this.from = from;
        this.to = to;
        this.orderCount = orderCount;
        this.grossRevenue = grossRevenue;
        this.cancellations = cancellations;
        this.refunds = refunds;
        this.bestSellers = bestSellers;
    }

    public double getNetRevenue() {
        return grossRevenue - refunds;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public ReportType getPeriod() {
        return period;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public double getGrossRevenue() {
        return grossRevenue;
    }

    public int getCancellations() {
        return cancellations;
    }

    public double getRefunds() {
        return refunds;
    }

    public List<ItemSales> getBestSellers() {
        return bestSellers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s SALES REPORT - %s (%s to %s)%n", period, restaurantName, from, to));
        sb.append(String.format("  Orders completed : %d%n", orderCount));
        sb.append(String.format("  Gross revenue    : PHP %.2f%n", grossRevenue));
        sb.append(String.format("  Cancellations    : %d%n", cancellations));
        sb.append(String.format("  Refunds          : PHP %.2f%n", refunds));
        sb.append(String.format("  Net revenue      : PHP %.2f%n", getNetRevenue()));
        sb.append("  Best-selling items (heap sort, highest to lowest):\n");
        if (bestSellers.isEmpty())
            sb.append("    (no sales in this period)\n");
        int rank = 1;
        for (ItemSales itemSale : bestSellers)
            sb.append(String.format("    %d. %-22s %3d sold  PHP %.2f%n", rank++, itemSale.getName(), itemSale.getUnits(), itemSale.getRevenue()));
        return sb.toString();
    }
}