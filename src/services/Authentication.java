package services;

import features.User;
import features.UserRole;
import java.util.ArrayDeque;
import java.util.Deque;

public class Authentication {
    private static User currentUser = null;
    private static final Deque<User> sessionStack = new ArrayDeque<>();

    public static User login(String email, String password) {
        for (User user : DataStoring.getUsers()) {
            if (user.getEmail().equalsIgnoreCase(email) && user.getPassword().equals(password)) {
                currentUser = user;
                sessionStack.push(user);
                return currentUser;
            }
        }
        return null;
    }

    public static void logout() {
        if (!sessionStack.isEmpty()) {
            sessionStack.pop();
        }
        currentUser = sessionStack.peek();
    }

    public static boolean checkRole(UserRole requiredRole) {
        return currentUser != null && currentUser.getRole() == requiredRole;
    }

    public static User getCurrentUser() {
        return currentUser;
    }
}
