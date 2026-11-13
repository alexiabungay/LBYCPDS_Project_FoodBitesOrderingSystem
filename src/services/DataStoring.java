package services;

import data.structure.AVLTree;
import features.Order;
import features.Restaurant;
import features.User;
import features.BusinessOwner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataStoring {
    private final AVLTree<String, User> usersByEmail = new AVLTree<>();
    private final AVLTree<Integer, User> usersById = new AVLTree<>();
    private final AVLTree<Integer, Restaurant> restaurants = new AVLTree<>();
    private final AVLTree<Integer, Order> orders = new AVLTree<>();

    private final RestaurantStorage restaurantStorage;

    public DataStoring() {
        this("data/restaurants");
    }

    public DataStoring(String restaurantFolder) {
        this.restaurantStorage = new RestaurantStorage(restaurantFolder);
    }

    private int nextUserId = 1;
    private int nextRestaurantId = 1;
    private int nextMenuItemId = 1;
    private int nextOrderId = 1001;

    public int newUserId() {
        return nextUserId++;
    }

    public int newRestaurantId() {
        return nextRestaurantId++;
    }

    public int newMenuItemId() {
        return nextMenuItemId++;
    }

    public int newOrderId() {
        return nextOrderId++;
    }

    public int loadRestaurants() {
        Map<Path, Integer> restaurantIdsByFile = new LinkedHashMap<>();
        Set<Integer> usedRestaurantIds = new HashSet<>();
        int highestRestaurantId = 0;

        for (Path csvFile : restaurantStorage.listFiles()) {
            int restaurantId = restaurantStorage.readId(csvFile);
            if (restaurantId > 0 && usedRestaurantIds.add(restaurantId)) {
                highestRestaurantId = Math.max(highestRestaurantId, restaurantId);
            } else {
                restaurantId = 0;
            }
            restaurantIdsByFile.put(csvFile, restaurantId);
        }
        nextRestaurantId = Math.max(nextRestaurantId, highestRestaurantId + 1);

        int loadedCount = 0;
        for (Map.Entry<Path, Integer> fileEntry : restaurantIdsByFile.entrySet()) {
            Path csvFile = fileEntry.getKey();
            boolean needsNewId = fileEntry.getValue() == 0;

            int restaurantId;
            if (needsNewId) {
                restaurantId = newRestaurantId();
            } else {
                restaurantId = fileEntry.getValue();
            }

            try {
                Restaurant restaurant = restaurantStorage.read(csvFile, restaurantId, this::newMenuItemId);
                restaurants.insert(restaurant.getId(), restaurant);
                restaurantStorage.register(restaurantId, csvFile);
                if (needsNewId) {
                    restaurantStorage.save(restaurant);   // writes the new id back to the file
                }
                loadedCount++;
            } catch (IOException | RuntimeException exception) {
                System.err.println("! Skipped " + csvFile.getFileName() + ": " + exception.getMessage());
            }
        }
        return loadedCount;
    }

    public void saveRestaurant(Restaurant restaurant) {
        try {
            restaurantStorage.save(restaurant);
        } catch (IOException exception) {
            System.err.println("! Could not save restaurant file: " + exception.getMessage());
        }
    }

    public void deleteRestaurantFile(Restaurant restaurant) {
        try {
            restaurantStorage.delete(restaurant);
        } catch (IOException exception) {
            System.err.println("! Could not delete restaurant file: " + exception.getMessage());
        }
    }

    public void relinkOwner(User owner) {
        for (Restaurant restaurant : restaurants.inOrder()) {
            boolean isOwnedByUser = restaurant.getOwnerEmail().equalsIgnoreCase(owner.getEmail());
            if (isOwnedByUser) {
                restaurant.setOwnerId(owner.getId());
                if (owner instanceof BusinessOwner) {
                    ((BusinessOwner) owner).getRestaurantIds().add(restaurant.getId());
                }
            }
        }
    }

    public AVLTree<String, User> getUsersByEmail() {
        return usersByEmail;
    }

    public AVLTree<Integer, User> getUsersById() {
        return usersById;
    }

    public AVLTree<Integer, Restaurant> getRestaurants() {
        return restaurants;
    }

    public AVLTree<Integer, Order> getOrders() {
        return orders;
    }
}