package services;

public final class Validator {
    private Validator() { }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^(\\+63|0)\\d{10}$");
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        return hasLetter && hasDigit;
    }

    public static boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
