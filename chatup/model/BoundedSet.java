package chatup.model;

import chatup.main.ChatupGlobals;

import java.util.TreeSet;

public class BoundedSet<E> extends TreeSet<E>
{
    private int mSize;

    public BoundedSet()
    {
        mSize = ChatupGlobals.DefaultCacheSize;
    }

    BoundedSet(int maxSize)
    {
        mSize = maxSize;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean add(E e)
    {
        if (size() >= mSize)
        {
            final E smallest = first();
            return ((Comparable<E>) e).compareTo(smallest) > 0 && remove(smallest) && super.add(e);
        }

        return super.add(e);
    }
}