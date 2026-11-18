package features;

public enum DiscountType {
    NONE("No discount"),
    SENIOR_CITIZEN("Senior Citizen"),
    PWD("PWD");

    private final String label;

    DiscountType(String label) {
        this.label = label;
    }

    public boolean isSectorDiscount() {
        return this != NONE;
    }

    @Override
    public String toString() {
        return label;
    }
}