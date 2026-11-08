package services;

import data.structure.AVLTree;
import data.structure.HeapSort;
import features.MenuItem;
import features.SortFeature;
import java.util.List;
import java.util.ArrayList;

public class Menu {
    private final AVLTree nameTree = new AVLTree(AVLTree.KEY_NAME);
    private final AVLTree priceTree = new AVLTree(AVLTree.KEY_PRICE);

    public void addMenuItem(String name, int price) {
        nameTree.insert(name, price);
        priceTree.insert(name, price);
    }
}
