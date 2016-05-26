package chatup.model;

import java.util.TreeSet;

class BoundedSet<E> extends TreeSet<E> {

    private int mSize;

    BoundedSet(int maxSize) {
        mSize = maxSize;
    }

    @Override
    public boolean add(E e) {

        if (size() >= mSize) {

            final E smallest = first();

            if (((Comparable<E>) e).compareTo(smallest) > 0) {
                return remove(smallest) && super.add(e);
            }

            return false;
        }

        return super.add(e);
    }
}