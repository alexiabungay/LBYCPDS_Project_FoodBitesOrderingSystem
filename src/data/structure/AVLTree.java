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
}
