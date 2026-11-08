package data.structure;

public class HeapSort {

    static int compareSales(AVLTree.TreeNode a, AVLTree.TreeNode b) {
        if (a.totalSales != b.totalSales) {
            return Integer.compare(a.totalSales, b.totalSales);
        }
        return AVLTree.compareNames(b.menuName, a.menuName);
    }

    static void heapify(AVLTree.TreeNode[] arr, int n, int i) {

        int largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;

        if (left < n && compareSales(arr[left], arr[largest]) > 0) {
            largest = left;
        }
        if (right < n && compareSales(arr[right], arr[largest]) > 0) {
            largest = right;
        }

        if (largest != i) {
            AVLTree.TreeNode temp = arr[i];
            arr[i] = arr[largest];
            arr[largest] = temp;

            heapify(arr, n, largest);
        }
    }

    static void buildMaxHeap(AVLTree.TreeNode[] arr) {
        for (int i = arr.length / 2 - 1; i >= 0; i--) {
            heapify(arr, arr.length, i);
        }
    }

    public static void heapSort(AVLTree.TreeNode[] arr) {
        buildMaxHeap(arr);

        for (int end = arr.length - 1; end > 0; end--) {
            AVLTree.TreeNode temp = arr[0];
            arr[0] = arr[end];
            arr[end] = temp;

            heapify(arr, end, 0);
        }
    }

    static void minHeapify(AVLTree.TreeNode[] arr, int n, int i) {

        int smallest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;

        if (left < n && compareSales(arr[left], arr[smallest]) < 0) {
            smallest = left;
        }
        if (right < n && compareSales(arr[right], arr[smallest]) < 0) {
            smallest = right;
        }

        if (smallest != i) {
            AVLTree.TreeNode temp = arr[i];
            arr[i] = arr[smallest];
            arr[smallest] = temp;

            minHeapify(arr, n, smallest);
        }
    }

    static void buildMinHeap(AVLTree.TreeNode[] arr) {
        for (int i = arr.length / 2 - 1; i >= 0; i--) {
            minHeapify(arr, arr.length, i);
        }
    }

    public static void minHeapSort(AVLTree.TreeNode[] arr) {
        buildMinHeap(arr);

        for (int end = arr.length - 1; end > 0; end--) {
            AVLTree.TreeNode temp = arr[0];
            arr[0] = arr[end];
            arr[end] = temp;

            minHeapify(arr, end, 0);
        }
    }

    static void printItems(String title, AVLTree.TreeNode[] arr) {
        System.out.println(title);
        for (int i = 0; i < arr.length; i++) {
            System.out.println("  " + arr[i].menuName + " - P" + arr[i].menuPrice + " (" + arr[i].totalSales + " sold)");
        }
    }

    public static void main(String[] args) {
        String[] names = {"Sisig", "Bacsilog", "1pc Chicken Joy", "Mang-Inasal Paa", "Chicken Burger McDo"};
        int[] prices = {190, 120, 110, 180, 100};
        int[] sold = {67, 420, 123, 1738, 1103};

        AVLTree.TreeNode[] items = new AVLTree.TreeNode[names.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = new AVLTree.TreeNode(names[i], prices[i]);
            items[i].totalSales = sold[i];
        }

        printItems("Before heap sort:", items);
        heapSort(items);
        printItems("\nAfter max-heap sort (lowest to highest sales):", items);
        minHeapSort(items);
        printItems("\nAfter min-heap sort (highest to lowest sales):", items);
    }
}