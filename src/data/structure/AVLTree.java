package data.structure;

public class AVLTree {

    public static final int KEY_NAME = 1;
    public static final int KEY_PRICE = 2;

    public static class TreeNode {
        TreeNode left;
        TreeNode right;
        String menuName;
        int menuPrice;
        int totalSales;
        int height;

        TreeNode(String menuName, int menuPrice) {
            this.menuName = menuName;
            this.menuPrice = menuPrice;
            this.totalSales = 0;
            this.height = 1;
        }

        public String getMenuName() {
            return menuName;
        }

        public int getMenuPrice() {
            return menuPrice;
        }

        public int getTotalSales() {
            return totalSales;
        }
    }

    TreeNode root;
    int keyType;
    int size;

    public AVLTree(int keyType) {
        if (keyType != KEY_NAME && keyType != KEY_PRICE) {
            throw new IllegalArgumentException("keyType must be AVLTree.BY_NAME or AVLTree.BY_PRICE");
        }
        this.keyType = keyType;
    }

    static int compareNames(String a, String b) {
        return a.compareToIgnoreCase(b);
    }

    static int comparePrices(int a, int b) {
        return Integer.compare(a, b);
    }

    int compareNodes(String menuName, int menuPrice, TreeNode node) {
        if (keyType == KEY_NAME) {
            int c = compareNames(menuName, node.menuName);
            if (c != 0) {
                return c;
            }
            return comparePrices(menuPrice, node.menuPrice);
        }

        int c = comparePrices(menuPrice, node.menuPrice);
        if (c != 0) {
            return c;
        }
        return compareNames(menuName, node.menuName);
    }

    static int height(TreeNode node) {
        return node == null ? 0 : node.height;
    }

    static void updateHeight(TreeNode node) {
        node.height = 1 + Math.max(height(node.left), height(node.right));
    }

    static int getBalance(TreeNode node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    static TreeNode rightRotate(TreeNode y) {
        TreeNode x = y.left;
        TreeNode t2 = x.right;

        x.right = y;
        y.left = t2;

        updateHeight(y);
        updateHeight(x);
        return x;
    }

    static TreeNode leftRotate(TreeNode x) {
        TreeNode y = x.right;
        TreeNode t2 = y.left;

        y.left = x;
        x.right = t2;

        updateHeight(x);
        updateHeight(y);
        return y;
    }

    static TreeNode rebalance(TreeNode node) {
        int balance = getBalance(node);

        if (balance > 1 && getBalance(node.left) >= 0) {
            return rightRotate(node);
        }
        if (balance > 1 && getBalance(node.left) < 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
        if (balance < -1 && getBalance(node.right) <= 0) {
            return leftRotate(node);
        }
        if (balance < -1 && getBalance(node.right) > 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }

    public boolean insert(String menuName, int menuPrice) {
        int before = size;
        root = insertRec(root, menuName, menuPrice);
        return size > before;
    }

    TreeNode insertRec(TreeNode node, String menuName, int menuPrice) {
        if (node == null) {
            size++;
            return new TreeNode(menuName, menuPrice);
        }

        int cmp = compareNodes(menuName, menuPrice, node);

        if (cmp < 0) {
            node.left = insertRec(node.left, menuName, menuPrice);
        } else if (cmp > 0) {
            node.right = insertRec(node.right, menuName, menuPrice);
        } else {
            return node;
        }

        updateHeight(node);
        return rebalance(node);
    }

    public static void preorder(TreeNode node) {
        if (node != null) {
            System.out.print(node.menuName + "(P" + node.menuPrice + ") ");
            inorder(node.left);
            inorder(node.right);
        }
    }

    public static void inorder(TreeNode node) {
        if (node != null) {
            inorder(node.left);
            System.out.print(node.menuName + "(P" + node.menuPrice + ") ");
            inorder(node.right);
        }
    }

    public static void postorder(TreeNode node) {
        if (node != null) {
            inorder(node.left);
            inorder(node.right);
            System.out.print(node.menuName + "(P" + node.menuPrice + ") ");
        }
    }

    public static void printTree(TreeNode node, String prefix, boolean isLeft, String side) {
        if (node != null) {
            String label = side.isEmpty() ? "" : "[" + side + "] ";
            System.out.println(prefix + (isLeft ? "├── " : "└── ") + label + node.menuName + " (P" + node.menuPrice + ")");

            if (node.left != null || node.right != null) {
                printTree(node.left, prefix + (isLeft ? "│   " : "    "), true, "L");
                if (node.right != null) {
                    printTree(node.right, prefix + (isLeft ? "│   " : "    "), false, "R");
                }
            }
        }
    }

    public static void main(String[] args) {
        String[] names = {"Sisig", "Bacsilog", "1pc Chicken Joy", "Mang-Inasal Paa", "Chicken Burger McDo"};
        int[] prices = {190, 120, 110, 180, 100};

        AVLTree nameTree = new AVLTree(KEY_NAME);
        AVLTree priceTree = new AVLTree(KEY_PRICE);

        for (int i = 0; i < names.length; i++) {
            nameTree.insert(names[i], prices[i]);
            priceTree.insert(names[i], prices[i]);
        }

        System.out.println("Name tree (A-Z):");
        printTree(nameTree.root, "", false, "");
        System.out.print("\nPre-order: ");
        preorder(nameTree.root);
        System.out.print("\nIn-order: ");
        inorder(nameTree.root);
        System.out.print("\nPost-order: ");
        postorder(nameTree.root);

        System.out.println("\n\nPrice tree (low to high):");
        printTree(priceTree.root, "", false, "");
        System.out.print("\nPre-order: ");
        preorder(priceTree.root);
        System.out.print("\nIn-order: ");
        inorder(priceTree.root);
        System.out.print("\nPost-order: ");
        postorder(priceTree.root);
    }
}
