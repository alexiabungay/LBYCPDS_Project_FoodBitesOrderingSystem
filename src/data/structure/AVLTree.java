package data.structure;

import java.util.ArrayList;
import java.util.List;

public class AVLTree<Key extends Comparable<Key>, Value> {

    private class Node {
        Key key;
        Value value;
        int height = 1;
        Node left, right;
        Node(Key key, Value value) { this.key = key; this.value = value; }
    }

    private Node root;
    private int size;
    private boolean removed;

    public void insert(Key key, Value value) {
        root = insert(root, key, value);
    }

    public Value search(Key key) {
        Node currentNode = root;
        while (currentNode != null) {
            int comparisonResult = key.compareTo(currentNode.key);
            if (comparisonResult == 0)
                return currentNode.value;
            currentNode = comparisonResult < 0 ? currentNode.left : currentNode.right;
        }
        return null;
    }

    public boolean contains(Key key) {
        return search(key) != null;
    }

    public boolean delete(Key key) {
        removed = false;
        root = delete(root, key);
        if (removed) size--;
        return removed;
    }

    public List<Value> inOrder() {
        List<Value> out = new ArrayList<>();
        inOrder(root, out);
        return out;
    }

    public List<Value> reverseInOrder() {
        List<Value> out = new ArrayList<>();
        reverseInOrder(root, out);
        return out;
    }

    public List<Value> range(Key lowerBound, Key upperBound) {
        List<Value> out = new ArrayList<>();
        range(root, lowerBound, upperBound, out);
        return out;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int height() {
        return height(root);
    }

    private int height(Node node) {
        return node == null ? 0 : node.height;
    }

    private void update(Node node) {
        node.height = 1 + Math.max(height(node.left), height(node.right));
    }

    private int balanceFactor(Node node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    private Node rotateRight(Node nodeY) {
        Node nodeX = nodeY.left;
        nodeY.left = nodeX.right;
        nodeX.right = nodeY;
        update(nodeY);
        update(nodeX);
        return nodeX;
    }

    private Node rotateLeft(Node nodeX) {
        Node nodeY = nodeX.right;
        nodeX.right = nodeY.left;
        nodeY.left = nodeX;
        update(nodeX);
        update(nodeY);
        return nodeY;
    }

    private Node rebalance(Node node) {
        update(node);
        int balance = balanceFactor(node);
        if (balance > 1) {
            if (balanceFactor(node.left) < 0)
                node.left = rotateLeft(node.left);
            return rotateRight(node);
        }
        if (balance < -1) {
            if (balanceFactor(node.right) > 0)
                node.right = rotateRight(node.right);
            return rotateLeft(node);
        }
        return node;
    }

    private Node insert(Node node, Key key, Value value) {
        if (node == null) {
            size++;
            return new Node(key, value);
        }
        int comparisonResult = key.compareTo(node.key);
        if (comparisonResult < 0) node.left = insert(node.left, key, value);
        else if (comparisonResult > 0) node.right = insert(node.right, key, value);
        else { node.value = value; return node; }
        return rebalance(node);
    }

    private Node delete(Node node, Key key) {
        if (node == null)
            return null;
        int comparisonResult = key.compareTo(node.key);
        if (comparisonResult < 0)
            node.left = delete(node.left, key);
        else if (comparisonResult > 0)
            node.right = delete(node.right, key);
        else {
            removed = true;
            if (node.left == null)
                return node.right;
            if (node.right == null)
                return node.left;
            Node min = node.right;
            while (min.left != null)
                min = min.left;
            node.key = min.key;
            node.value = min.value;
            node.right = delete(node.right, min.key);
        }
        return rebalance(node);
    }

    private void inOrder(Node node, List<Value> out) {
        if (node == null)
            return;
        inOrder(node.left, out);
        out.add(node.value);
        inOrder(node.right, out);
    }

    private void reverseInOrder(Node node, List<Value> out) {
        if (node == null)
            return;
        reverseInOrder(node.right, out);
        out.add(node.value);
        reverseInOrder(node.left, out);
    }

    private void range(Node node, Key lowerBound, Key upperBound, List<Value> out) {
        if (node == null)
            return;
        if (lowerBound.compareTo(node.key) < 0)
            range(node.left, lowerBound, upperBound, out);
        if (lowerBound.compareTo(node.key) <= 0 && upperBound.compareTo(node.key) >= 0)
            out.add(node.value);
        if (upperBound.compareTo(node.key) > 0)
            range(node.right, lowerBound, upperBound, out);
    }
}