package data.structure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class LinkedListQueue<T> implements Iterable<T> {

    private static class Node<T> {
        T data;
        Node<T> next;
        Node(T data) { this.data = data; }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public void enqueue(T item) {
        Node<T> node = new Node<>(item);
        if (tail == null) {
            head = tail = node;
        } else {
            tail.next = node;
            tail = node;
        }
        size++;
    }

    public T dequeue() {
        if (head == null) return null;
        T data = head.data;
        head = head.next;
        if (head == null) tail = null;
        size--;
        return data;
    }

    public T peek() {
        return head == null ? null : head.data;
    }

    public boolean remove(T item) {
        Node<T> prev = null;
        Node<T> cur = head;
        while (cur != null) {
            if (cur.data.equals(item)) {
                if (prev == null) head = cur.next;
                else prev.next = cur.next;
                if (cur == tail) tail = prev;
                size--;
                return true;
            }
            prev = cur;
            cur = cur.next;
        }
        return false;
    }

    public int positionOf(T item) {
        int pos = 1;
        for (Node<T> cur = head; cur != null; cur = cur.next, pos++) {
            if (cur.data.equals(item)) return pos;
        }
        return -1;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() { return size == 0; }

    public List<T> toList() {
        List<T> list = new ArrayList<>();
        for (T t : this)
            list.add(t);
        return list;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> cur = head;
            public boolean hasNext() {
                return cur != null;
            }

            public T next() {
                if (cur == null) throw new NoSuchElementException();
                T d = cur.data;
                cur = cur.next;
                return d;
            }
        };
    }
}