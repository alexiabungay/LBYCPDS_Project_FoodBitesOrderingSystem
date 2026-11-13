package services;

import features.*;

public class Authentication {
    private final DataStoring store;
    private User currentUser;

    public Authentication(DataStoring store) {
        this.store = store;
    }

    public User register(String name, String email, String phone, String password, UserRole role, int restaurantId) {
        if (!Validator.isNotBlank(name)) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (!Validator.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        if (!Validator.isValidPhone(phone)) {
            throw new IllegalArgumentException("Phone must be like a string of integers.");
        }
        if (!Validator.isValidPassword(password)) {
            throw new IllegalArgumentException("Password needs at least 8 characters with a letter and a number.");
        }
        if (store.getUsersByEmail().contains(email.toLowerCase())) {
            throw new IllegalArgumentException("That email is already registered.");
        }
        if (role == UserRole.ADMINISTRATOR) {
            throw new IllegalArgumentException("Administrator accounts cannot be self-registered.");
        }

        int userId = store.newUserId();
        User newUser;
}
