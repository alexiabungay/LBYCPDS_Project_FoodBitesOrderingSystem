package services;

import data.structure.AVLTree;
import data.structure.HeapSort;
import features.*;

import java.util.*;

public class Menu {
    private final DataStoring store;

    public Menu(DataStoring store) {
        this.store = store;
    }

    public List<Restaurant> browseRestaurants() {
        return store.getRestaurants().inOrder();
    }

    public List<Restaurant> browseRestaurantsAlphabetical() {
        AVLTree<String, Restaurant> byName = new AVLTree<>();
        for (Restaurant restaurant : browseRestaurants()) {
            byName.insert(String.format("%s#%06d", restaurant.getName().toLowerCase(), restaurant.getId()), restaurant);
        }
        return byName.inOrder();
    }

    public List<Restaurant> browseRestaurantsBySalesAscending() {
        Restaurant[] restaurants = browseRestaurants().toArray(new Restaurant[0]);
        HeapSort.sortAscending(restaurants, Comparator.comparingDouble(Restaurant::getTotalSales));
        return new ArrayList<>(java.util.Arrays.asList(restaurants));
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

    // Business Owner Menu

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
        if (addOn == null || !Validator.isNotBlank(addOn.getName())) {
            throw new IllegalArgumentException("Upgrade name is required.");
        }
        if (addOn.getPrice() < 0) {
            throw new IllegalArgumentException("Upgrade price cannot be negative.");
        }
        MenuItem menuItem = findItem(actor, restaurantId, itemName);
        for (AddOn existing : menuItem.getAddOns()) {
            if (existing.getName().equalsIgnoreCase(addOn.getName().trim())) {
                throw new IllegalArgumentException("That upgrade already exists for this item.");
            }
        }
        menuItem.getAddOns().add(new AddOn(addOn.getName().trim(), addOn.getPrice()));
        store.saveRestaurant(getRestaurant(restaurantId));
    }

    public void addPortionSize(User actor, int restaurantId, String itemName, String portionSize) {
        if (!Validator.isNotBlank(portionSize)) {
            throw new IllegalArgumentException("Portion size is required.");
        }
        MenuItem menuItem = findItem(actor, restaurantId, itemName);
        for (String existing : menuItem.getPortionSizes()) {
            if (existing.equalsIgnoreCase(portionSize.trim())) {
                throw new IllegalArgumentException("That portion size already exists for this item.");
            }
        }
        menuItem.getPortionSizes().add(portionSize.trim());
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

    // Admin Menu

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

        User owner = store.getUsersById().search(restaurant.getOwnerId());
        if (owner instanceof BusinessOwner) {
            ((BusinessOwner) owner).getRestaurantIds().remove((Integer) restaurantId);
        }
    }

    public List<BusinessOwner> browseBusinessOwners() {
        List<BusinessOwner> owners = new ArrayList<>();
        for (User user : store.getUsersById().inOrder()) {
            if (user instanceof BusinessOwner) {
                owners.add((BusinessOwner) user);
            }
        }
        return owners;
    }

    private BusinessOwner findBusinessOwner(String ownerEmail) {
        User found = ownerEmail == null ? null : store.getUsersByEmail().search(ownerEmail.trim().toLowerCase());
        if (!(found instanceof BusinessOwner)) {
            throw new IllegalArgumentException("No business owner account has that email.");
        }
        return (BusinessOwner) found;
    }

    private void requireAdministrator(User actor) {
        if (actor == null || actor.getRole() != UserRole.ADMINISTRATOR) {
            throw new SecurityException("Only administrators can assign restaurant owners.");
        }
    }

    public void assignOwner(User actor, int restaurantId, String ownerEmail) {
        requireAdministrator(actor);
        Restaurant restaurant = getRestaurant(restaurantId);
        BusinessOwner newOwner = findBusinessOwner(ownerEmail);

        User oldOwner = store.getUsersById().search(restaurant.getOwnerId());
        if (oldOwner instanceof BusinessOwner) {
            ((BusinessOwner) oldOwner).getRestaurantIds().remove((Integer) restaurantId);
        }
        restaurant.setOwnerId(newOwner.getId());
        restaurant.setOwnerEmail(newOwner.getEmail());
        if (!newOwner.getRestaurantIds().contains(restaurantId)) {
            newOwner.getRestaurantIds().add(restaurantId);
        }
        store.saveRestaurant(restaurant);
    }

    public Restaurant createRestaurant(User actor, String name, String description, String address, String ownerEmail) {
        requireAdministrator(actor);
        BusinessOwner owner = findBusinessOwner(ownerEmail);
        Restaurant restaurant = createRestaurant(actor, name, description, address);
        assignOwner(actor, restaurant.getId(), owner.getEmail());
        return restaurant;
    }

    public void updateRestaurant(User actor, int restaurantId, String name, String description, String address, String ownerEmail) {
        requireAdministrator(actor);
        boolean changeOwner = Validator.isNotBlank(ownerEmail);
        if (changeOwner) {
            findBusinessOwner(ownerEmail);
        }
        updateRestaurant(actor, restaurantId, name, description, address);
        if (changeOwner) {
            assignOwner(actor, restaurantId, ownerEmail);
        }
    }
}