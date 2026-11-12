package features;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public abstract class User {
    private final int id;
    private String name;
    private final String email;
    private String phone;
    private final String passwordHash;
    private final UserRole role;

    protected User(int id, String name, String email, String phone, String password, UserRole role) {
        this.id = id;
        this.name = name;
        this.email = email.toLowerCase();
        this.phone = phone;
        this.passwordHash = hash(password);
        this.role = role;
    }

    public boolean checkPassword(String password) {
        return passwordHash.equals(hash(password));
    }

    private static String hash(String input) {
        try {
            MessageDigest message = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte currentByte : message.digest(input.getBytes(StandardCharsets.UTF_8))) sb.append(String.format("%02x", currentByte));
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }

    public abstract List<String> getAllowedActions();

    public abstract String getRoleDescription();

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public String toString() {
        return name + " (" + email + ", " + role + ")";
    }
}