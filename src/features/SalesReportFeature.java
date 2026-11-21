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
        sb.append(services.DisplayMenu.forSalesSummary(new String[][]{
                {"Orders completed", String.valueOf(orderCount)},
                {"Gross revenue", String.format("PHP %.2f", grossRevenue)},
                {"Cancellations", String.valueOf(cancellations)},
                {"Refunds", String.format("PHP %.2f", refunds)},
                {"Net revenue", String.format("PHP %.2f", getNetRevenue())}
        })).append(System.lineSeparator());
        sb.append(System.lineSeparator()).append("Best-selling items (highest to lowest)").append(System.lineSeparator());
        if (bestSellers.isEmpty()) {
            sb.append("(no sales in this period)").append(System.lineSeparator());
        } else {
            sb.append(services.DisplayMenu.forSalesItems(bestSellers)).append(System.lineSeparator());
        }
        return sb.toString();
    }
}