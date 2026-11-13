package services;

import features.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Menu {
    private final DataStoring store;

    public Menu(DataStoring store) {
        this.store = store;
    }

    public List<Restaurant> browseRestaurants() {
        return store.getRestaurants().inOrder();
    }

    public Restaurant getRestaurant(int restaurantId) {
        Restaurant restaurant = store.getRestaurants().search(restaurantId);
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant not found.");
        }
        return restaurant;
    }

    public Map<Category, List<MenuItem>> browseMenu(Restaurant restaurant) {
        Map<Category, List<MenuItem>> itemsByCategory = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            itemsByCategory.put(category, new ArrayList<>());
        }
        for (MenuItem menuItem : restaurant.listAlphabetical()) {
            itemsByCategory.get(menuItem.getCategory()).add(menuItem);
        }
        return itemsByCategory;
    }

    public List<MenuItem> search(Restaurant restaurant, String keyword) {
        return restaurant.searchByName(keyword);
    }

    /** Searches every restaurant so customers can compare prices. */
    public List<MenuItem> searchAll(String keyword) {
        List<MenuItem> matchingItems = new ArrayList<>();
        for (Restaurant restaurant : browseRestaurants()) {
            matchingItems.addAll(restaurant.searchByName(keyword));
        }
        return matchingItems;
    }

    public List<MenuItem> filterByCategory(Restaurant restaurant, Category category) {
        return restaurant.filterByCategory(category);
    }

    public List<MenuItem> filterByPrice(Restaurant restaurant, double minimumPrice, double maximumPrice) {
        return restaurant.filterByPriceRange(minimumPrice, maximumPrice);
    }

    public List<MenuItem> sort(Restaurant restaurant, SortFeature sortMode) {
        switch (sortMode) {
            case PRICE_LOW_TO_HIGH:
                return restaurant.listByPriceAscending();
            case PRICE_HIGH_TO_LOW:
                return restaurant.listByPriceDescending();
            case POPULARITY:
                return restaurant.listByPopularity();
            default:
                return restaurant.listAlphabetical();
        }
    }

    public Restaurant createRestaurant(User actor, String name, String description, String address) {
        requireManager(actor);
        if (!Validator.isNotBlank(name)) {
            throw new IllegalArgumentException("Restaurant name is required.");
        }

        int ownerId = actor.getId();
        Restaurant restaurant = new Restaurant(store.newRestaurantId(), ownerId, name, description, address);
        restaurant.setOwnerEmail(actor.getEmail());
        store.getRestaurants().insert(restaurant.getId(), restaurant);

        if (actor instanceof BusinessOwner) {
            ((BusinessOwner) actor).getRestaurantIds().add(restaurant.getId());
        }
        store.saveRestaurant(restaurant);
        return restaurant;
    }

    public void updateRestaurant(User actor, int restaurantId, String name, String description, String address) {
        Restaurant restaurant = requireOwnership(actor, restaurantId);
        restaurant.setName(name);
        restaurant.setDescription(description);
        restaurant.setAddress(address);
        store.saveRestaurant(restaurant);
    }

    public void removeRestaurant(User actor, int restaurantId) {
        Restaurant restaurant = requireOwnership(actor, restaurantId);
        store.getRestaurants().delete(restaurantId);
        store.deleteRestaurantFile(restaurant);

        if (actor instanceof BusinessOwner) {
            ((BusinessOwner) actor).getRestaurantIds().remove((Integer) restaurantId);
        }
    }

    public MenuItem addItem(User actor, int restaurantId, String name, String description,
                            Category category, double price, int stock) {
        Restaurant restaurant = requireOwnership(actor, restaurantId);
        if (!Validator.isNotBlank(name)) {
            throw new IllegalArgumentException("Item name is required.");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }

        MenuItem menuItem = new MenuItem(store.newMenuItemId(), restaurantId, name, description, category, price, stock);
        if (!restaurant.addMenuItem(menuItem)) {
            throw new IllegalArgumentException("An item with that name already exists.");
        }
        store.saveRestaurant(restaurant);
        return menuItem;
    }

    public void editItem(User actor, int restaurantId, String oldName, String newName,
                         String description, Category category, double price) {
        Restaurant restaurant = requireOwnership(actor, restaurantId);
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }

        boolean wasEdited = restaurant.editMenuItem(oldName, newName, description, category, price);
        if (!wasEdited) {
            throw new IllegalArgumentException("Item not found or new name already used.");
        }
        store.saveRestaurant(restaurant);
    }

    public void updatePrice(User actor, int restaurantId, String itemName, double price) {
        Restaurant restaurant = requireOwnership(actor, restaurantId);
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }
        if (!restaurant.updatePrice(itemName, price)) {
            throw new IllegalArgumentException("Item not found.");
        }
        store.saveRestaurant(restaurant);
    }

    public void removeItem(User actor, int restaurantId, String itemName) {
        Restaurant restaurant = requireOwnership(actor, restaurantId);
        if (!restaurant.removeMenuItem(itemName)) {
            throw new IllegalArgumentException("Item not found.");
        }
        store.saveRestaurant(restaurant);
    }

    public void setAvailability(User actor, int restaurantId, String itemName, boolean available) {
        MenuItem menuItem = findItem(actor, restaurantId, itemName);
        menuItem.setAvailable(available);
        store.saveRestaurant(getRestaurant(restaurantId));
    }

    public void updateStock(User actor, int restaurantId, String itemName, int newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }
        findItem(actor, restaurantId, itemName).setStock(newStock);
        store.saveRestaurant(getRestaurant(restaurantId));
    }

    public void addAddOn(User actor, int restaurantId, String itemName, AddOn addOn) {
        findItem(actor, restaurantId, itemName).getAddOns().add(addOn);
        store.saveRestaurant(getRestaurant(restaurantId));
    }

    public void addPortionSize(User actor, int restaurantId, String itemName, String portionSize) {
        findItem(actor, restaurantId, itemName).getPortionSizes().add(portionSize);
        store.saveRestaurant(getRestaurant(restaurantId));
    }

    private MenuItem findItem(User actor, int restaurantId, String itemName) {
        Restaurant restaurant = requireOwnership(actor, restaurantId);
        MenuItem menuItem = restaurant.findByName(itemName);
        if (menuItem == null) {
            throw new IllegalArgumentException("Item not found.");
        }
        return menuItem;
    }

    private void requireManager(User actor) {
        boolean isManager = actor != null
                && (actor.getRole() == UserRole.BUSINESS_OWNER || actor.getRole() == UserRole.ADMINISTRATOR);
        if (!isManager) {
            throw new SecurityException("Only business owners and administrators can manage restaurants.");
        }
    }

    private Restaurant requireOwnership(User actor, int restaurantId) {
        requireManager(actor);
        Restaurant restaurant = getRestaurant(restaurantId);
        if (actor.getRole() == UserRole.BUSINESS_OWNER && restaurant.getOwnerId() != actor.getId()) {
            throw new SecurityException("You can only manage your own restaurant.");
        }
        return restaurant;
    }
}
