package data.structure;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class HeapSort {

    private HeapSort() { }

    public static <T> void sortAscending(T[] array, Comparator<? super T> cmp) {
        int arrayLength = array.length;
        for (int i = arrayLength / 2 - 1; i >= 0; i--)
            siftDown(array, arrayLength, i, cmp);
        for (int end = arrayLength - 1; end > 0; end--) {
            swap(array, 0, end);
            siftDown(array, end, 0, cmp);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> sortDescending(List<T> list, Comparator<? super T> cmp) {
        T[] arr = (T[]) list.toArray();
        sortAscending(arr, cmp);
        List<T> out = new java.util.ArrayList<>(list.size());
        for (T temp : arr)
            out.add(temp);
        Collections.reverse(out);
        return out;
    }

    private static <T> void siftDown(T[] array, int length, int i, Comparator<? super T> cmp) {
        while (true) {
            int largest = i;
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            if (left < length && cmp.compare(array[left], array[largest]) > 0)
                largest = left;
            if (right < length && cmp.compare(array[right], array[largest]) > 0)
                largest = right;
            if (largest == i)
                return;
            swap(array, i, largest);
            i = largest;
        }
    }

    private static <T> void swap(T[] array, int i, int j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
}