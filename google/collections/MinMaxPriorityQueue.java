package google.collections;

/*
 * Copyright (C) 2010 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*

        import com.google.common.math.IntMath;
        import com.google.j2objc.annotations.Weak;
        import com.google.j2objc.annotations.WeakOuter;

        import java.util.AbstractQueue;
        import java.util.ArrayDeque;
        import java.util.ArrayList;
        import java.util.Collection;
        import java.util.Collections;
        import java.util.Comparator;
        import java.util.ConcurrentModificationException;
        import java.util.Iterator;
        import java.util.List;
        import java.util.NoSuchElementException;
        import java.util.PriorityQueue;
        import java.util.Queue;


public final class MinMaxPriorityQueue<E> extends AbstractQueue<E> {

    public static <E extends Comparable<E>> MinMaxPriorityQueue<E> create() {
        return new Builder<Comparable>(Ordering.natural()).create();
    }

    public static <E extends Comparable<E>> MinMaxPriorityQueue<E> create(
            Iterable<? extends E> initialContents) {
        return new Builder<E>(Ordering.<E>natural()).create(initialContents);
    }

    public static <B> Builder<B> orderedBy(Comparator<B> comparator) {
        return new Builder<B>(comparator);
    }

    public static Builder<Comparable> expectedSize(int expectedSize) {
        return new Builder<Comparable>(Ordering.natural()).expectedSize(expectedSize);
    }

    public static Builder<Comparable> maximumSize(int maximumSize) {
        return new Builder<Comparable>(Ordering.natural()).maximumSize(maximumSize);
    }

    public static final class Builder<B> {

        private static final int UNSET_EXPECTED_SIZE = -1;

        private final Comparator<B> comparator;
        private int expectedSize = UNSET_EXPECTED_SIZE;
        private int maximumSize = Integer.MAX_VALUE;

        private Builder(Comparator<B> comparator) {
            this.comparator = comparator;
        }

        public Builder<B> expectedSize(int expectedSize) {
            this.expectedSize = expectedSize;
            return this;
        }

        public Builder<B> maximumSize(int maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        public <T extends B> MinMaxPriorityQueue<T> create() {
            return create(Collections.<T>emptySet());
        }

        public <T extends B> MinMaxPriorityQueue<T> create(Iterable<? extends T> initialContents) {

            final MinMaxPriorityQueue<T> queue = new MinMaxPriorityQueue<T>(this, initialQueueSize(expectedSize, maximumSize, initialContents));

            for (T element : initialContents) {
                queue.offer(element);
            }

            return queue;
        }

        @SuppressWarnings("unchecked")
        private <T extends B> Ordering<T> ordering() {
            return Ordering.from((Comparator<T>) comparator);
        }
    }

    private final Heap minHeap;
    private final Heap maxHeap;
    final int maximumSize;
    private Object[] queue;
    private int size;
    private int modCount;

    private MinMaxPriorityQueue(Builder<? super E> builder, int queueSize) {
        Ordering<E> ordering = builder.ordering();
        this.minHeap = new Heap(ordering);
        this.maxHeap = new Heap(ordering.reverse());
        minHeap.otherHeap = maxHeap;
        maxHeap.otherHeap = minHeap;
        this.maximumSize = builder.maximumSize;
        this.queue = new Object[queueSize];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean add(E element) {
        offer(element);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> newElements) {

        boolean modified = false;

        for (E element : newElements) {
            offer(element);
            modified = true;
        }

        return modified;
    }

    @Override
    public boolean offer(E element) {
        modCount++;
        int insertIndex = size++;

        growIfNeeded();
        heapForIndex(insertIndex).bubbleUp(insertIndex, element);
        return size <= maximumSize || pollLast() != element;
    }

    @Override
    public E poll() {
        return isEmpty() ? null : removeAndGet(0);
    }

    @SuppressWarnings("unchecked")
    E elementData(int index) {
        return (E) queue[index];
    }

    @Override
    public E peek() {
        return isEmpty() ? null : elementData(0);
    }

    private int getMaxElementIndex() {

        switch (size) {
        case 1:
            return 0;
        case 2:
            return 1;
        }

        return (maxHeap.compareElements(1, 2) <= 0) ? 1 : 2;
    }

    public E pollFirst() {
        return poll();
    }

    public E removeFirst() {
        return remove();
    }

    public E peekFirst() {
        return peek();
    }

    public E pollLast() {
        return isEmpty() ? null : removeAndGet(getMaxElementIndex());
    }

    public E removeLast() {

        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        return removeAndGet(getMaxElementIndex());
    }

    public E peekLast() {
        return isEmpty() ? null : elementData(getMaxElementIndex());
    }

    MoveDesc<E> removeAt(int index) {

        modCount++;
        size--;

        if (size == index) {
            queue[size] = null;
            return null;
        }
        E actualLastElement = elementData(size);
        int lastElementAt = heapForIndex(size).getCorrectLastElement(actualLastElement);
        E toTrickle = elementData(size);
        queue[size] = null;

        MoveDesc<E> changes = fillHole(index, toTrickle);

        if (lastElementAt < index) {

            if (changes == null) {
                return new MoveDesc<E>(actualLastElement, toTrickle);
            }
            else {
                return new MoveDesc<E>(actualLastElement, changes.replaced);
            }
        }

        return changes;
    }

    private MoveDesc<E> fillHole(int index, E toTrickle) {

        Heap heap = heapForIndex(index);

        int vacated = heap.fillHoleAt(index);
        int bubbledTo = heap.bubbleUpAlternatingLevels(vacated, toTrickle);

        if (bubbledTo == vacated) {
            return heap.tryCrossOverAndBubbleUp(index, vacated, toTrickle);
        }
        else {
            return (bubbledTo < index) ? new MoveDesc<E>(toTrickle, elementData(index)) : null;
        }
    }

    static class MoveDesc<E> {

        final E toTrickle;
        final E replaced;

        MoveDesc(E toTrickle, E replaced) {
            this.toTrickle = toTrickle;
            this.replaced = replaced;
        }
    }

    private E removeAndGet(int index) {
        E value = elementData(index);
        removeAt(index);
        return value;
    }

    private Heap heapForIndex(int i) {
        return isEvenLevel(i) ? minHeap : maxHeap;
    }

    private static final int EVEN_POWERS_OF_TWO = 0x55555555;
    private static final int ODD_POWERS_OF_TWO = 0xaaaaaaaa;

    static boolean isEvenLevel(int index) {
        int oneBased = index + 1;
        return (oneBased & EVEN_POWERS_OF_TWO) > (oneBased & ODD_POWERS_OF_TWO);
    }

    boolean isIntact() {

        for (int i = 1; i < size; i++) {

            if (!heapForIndex(i).verifyIndex(i)) {
                return false;
            }
        }

        return true;
    }

    @WeakOuter
    private class Heap {

        final Ordering<E> ordering;
        @Weak Heap otherHeap;

        Heap(Ordering<E> ordering) {
            this.ordering = ordering;
        }

        int compareElements(int a, int b) {
            return ordering.compare(elementData(a), elementData(b));
        }

        MoveDesc<E> tryCrossOverAndBubbleUp(int removeIndex, int vacated, E toTrickle) {

            int crossOver = crossOver(vacated, toTrickle);

            if (crossOver == vacated) {
                return null;
            }

            E parent;

            if (crossOver < removeIndex) {
                parent = elementData(removeIndex);
            }
            else {
                parent = elementData(getParentIndex(removeIndex));
            }

            if (otherHeap.bubbleUpAlternatingLevels(crossOver, toTrickle) < removeIndex) {
                return new MoveDesc<E>(toTrickle, parent);
            }
            else {
                return null;
            }
        }

        void bubbleUp(int index, E x) {

            int crossOver = crossOverUp(index, x);

            Heap heap;

            if (crossOver == index) {
                heap = this;
            }
            else {
                index = crossOver;
                heap = otherHeap;
            }

            heap.bubbleUpAlternatingLevels(index, x);
        }

        int bubbleUpAlternatingLevels(int index, E x) {

            while (index > 2) {

                int grandParentIndex = getGrandparentIndex(index);
                E e = elementData(grandParentIndex);

                if (ordering.compare(e, x) <= 0) {
                    break;
                }

                queue[index] = e;
                index = grandParentIndex;
            }

            queue[index] = x;

            return index;
        }

        int findMin(int index, int len) {

            if (index >= size) {
                return -1;
            }

            int limit = Math.min(index, size - len) + len;
            int minIndex = index;

            for (int i = index + 1; i < limit; i++) {

                if (compareElements(i, minIndex) < 0) {
                    minIndex = i;
                }
            }

            return minIndex;
        }

        int findMinChild(int index) {
            return findMin(getLeftChildIndex(index), 2);
        }

        int findMinGrandChild(int index) {

            int leftChildIndex = getLeftChildIndex(index);

            if (leftChildIndex < 0) {
                return -1;
            }

            return findMin(getLeftChildIndex(leftChildIndex), 4);
        }

        int crossOverUp(int index, E x) {

            if (index == 0) {
                queue[0] = x;
                return 0;
            }

            int parentIndex = getParentIndex(index);

            E parentElement = elementData(parentIndex);

            if (parentIndex != 0) {

                int grandparentIndex = getParentIndex(parentIndex);
                int uncleIndex = getRightChildIndex(grandparentIndex);

                if (uncleIndex != parentIndex && getLeftChildIndex(uncleIndex) >= size) {

                    E uncleElement = elementData(uncleIndex);

                    if (ordering.compare(uncleElement, parentElement) < 0) {
                        parentIndex = uncleIndex;
                        parentElement = uncleElement;
                    }
                }
            }

            if (ordering.compare(parentElement, x) < 0) {
                queue[index] = parentElement;
                queue[parentIndex] = x;
                return parentIndex;
            }

            queue[index] = x;

            return index;
        }

        int getCorrectLastElement(E actualLastElement) {

            int parentIndex = getParentIndex(size);

            if (parentIndex != 0) {

                int grandparentIndex = getParentIndex(parentIndex);
                int uncleIndex = getRightChildIndex(grandparentIndex);

                if (uncleIndex != parentIndex && getLeftChildIndex(uncleIndex) >= size) {

                    E uncleElement = elementData(uncleIndex);

                    if (ordering.compare(uncleElement, actualLastElement) < 0) {
                        queue[uncleIndex] = actualLastElement;
                        queue[size] = uncleElement;
                        return uncleIndex;
                    }
                }
            }
            return size;
        }

        int crossOver(int index, E x) {

            int minChildIndex = findMinChild(index);

            if ((minChildIndex > 0) && (ordering.compare(elementData(minChildIndex), x) < 0)) {
                queue[index] = elementData(minChildIndex);
                queue[minChildIndex] = x;
                return minChildIndex;
            }

            return crossOverUp(index, x);
        }

        int fillHoleAt(int index) {

            int minGrandchildIndex;

            while ((minGrandchildIndex = findMinGrandChild(index)) > 0) {
                queue[index] = elementData(minGrandchildIndex);
                index = minGrandchildIndex;
            }

            return index;
        }

        private boolean verifyIndex(int i) {

            if ((getLeftChildIndex(i) < size) && (compareElements(i, getLeftChildIndex(i)) > 0)) {
                return false;
            }

            if ((getRightChildIndex(i) < size) && (compareElements(i, getRightChildIndex(i)) > 0)) {
                return false;
            }

            if ((i > 0) && (compareElements(i, getParentIndex(i)) > 0)) {
                return false;
            }

            if ((i > 2) && (compareElements(getGrandparentIndex(i), i) > 0)) {
                return false;
            }

            return true;
        }

        private int getLeftChildIndex(int i) {
            return i * 2 + 1;
        }

        private int getRightChildIndex(int i) {
            return i * 2 + 2;
        }

        private int getParentIndex(int i) {
            return (i - 1) / 2;
        }

        private int getGrandparentIndex(int i) {
            return getParentIndex(getParentIndex(i)); // (i - 3) / 4
        }
    }

    private class QueueIterator implements Iterator<E> {

        private int cursor = -1;
        private int expectedModCount = modCount;
        private Queue<E> forgetMeNot;
        private List<E> skipMe;
        private E lastFromForgetMeNot;
        private boolean canRemove;

        @Override
        public boolean hasNext() {
            checkModCount();
            return (nextNotInSkipMe(cursor + 1) < size()) || ((forgetMeNot != null) && !forgetMeNot.isEmpty());
        }

        @Override
        public E next() {

            checkModCount();

            int tempCursor = nextNotInSkipMe(cursor + 1);

            if (tempCursor < size()) {
                cursor = tempCursor;
                canRemove = true;
                return elementData(cursor);
            }
            else if (forgetMeNot != null) {
                cursor = size();
                lastFromForgetMeNot = forgetMeNot.poll();

                if (lastFromForgetMeNot != null) {
                    canRemove = true;
                    return lastFromForgetMeNot;
                }
            }
            throw new NoSuchElementException("iterator moved past last element in queue.");
        }

        @Override
        public void remove() {

            checkModCount();
            canRemove = false;
            expectedModCount++;

            if (cursor < size()) {

                final MoveDesc<E> moved = removeAt(cursor);

                if (moved != null) {

                    if (forgetMeNot == null) {
                        forgetMeNot = new ArrayDeque<E>();
                        skipMe = new ArrayList<E>(3);
                    }

                    forgetMeNot.add(moved.toTrickle);
                    skipMe.add(moved.replaced);
                }

                cursor--;
            }
            else {
                lastFromForgetMeNot = null;
            }
        }

        private boolean containsExact(Iterable<E> elements, E target) {

            for (E element : elements) {

                if (element == target) {
                    return true;
                }
            }

            return false;
        }

        boolean removeExact(Object target) {

            for (int i = 0; i < size; i++) {

                if (queue[i] == target) {
                    removeAt(i);
                    return true;
                }
            }

            return false;
        }

        void checkModCount() {

            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        private int nextNotInSkipMe(int c) {

            if (skipMe != null) {

                while (c < size() && containsExact(skipMe, elementData(c))) {
                    c++;
                }
            }

            return c;
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new QueueIterator();
    }

    @Override
    public void clear() {

        for (int i = 0; i < size; i++) {
            queue[i] = null;
        }

        size = 0;
    }

    @Override
    public Object[] toArray() {
        Object[] copyTo = new Object[size];
        System.arraycopy(queue, 0, copyTo, 0, size);
        return copyTo;
    }

    public Comparator<? super E> comparator() {
        return minHeap.ordering;
    }

    int capacity() {
        return queue.length;
    }

    private static final int DEFAULT_CAPACITY = 11;

    static int initialQueueSize(
            int configuredExpectedSize, int maximumSize, Iterable<?> initialContents) {

        int result =
                (configuredExpectedSize == Builder.UNSET_EXPECTED_SIZE)
                        ? DEFAULT_CAPACITY
                        : configuredExpectedSize;

        if (initialContents instanceof Collection) {
            int initialSize = ((Collection<?>) initialContents).size();
            result = Math.max(result, initialSize);
        }

        return capAtMaximumSize(result, maximumSize);
    }

    private void growIfNeeded() {
        if (size > queue.length) {
            int newCapacity = calculateNewCapacity();
            Object[] newQueue = new Object[newCapacity];
            System.arraycopy(queue, 0, newQueue, 0, queue.length);
            queue = newQueue;
        }
    }

    private int calculateNewCapacity() {
        int oldCapacity = queue.length;
        int newCapacity =
                (oldCapacity < 64)
                        ? (oldCapacity + 1) * 2
                        : IntMath.checkedMultiply(oldCapacity / 2, 3);
        return capAtMaximumSize(newCapacity, maximumSize);
    }

    private static int capAtMaximumSize(int queueSize, int maximumSize) {
        return Math.min(queueSize - 1, maximumSize) + 1; // don't overflow
    }
}*/