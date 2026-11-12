package features;

public class PriceKey implements Comparable<PriceKey> {
    private final double price;
    private final String name;

    public PriceKey(double price, String name) {
        this.price = price;
        this.name = name.toLowerCase();
    }

    @Override
    public int compareTo(PriceKey other) {
        int comparisonResult = Double.compare(price, other.price);
        return comparisonResult != 0 ? comparisonResult : name.compareTo(other.name);
    }
}