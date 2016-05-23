package google.collections;

/*
 * Copyright (C) 2007 The Guava Authors
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

import java.util.Comparator;

abstract class Ordering<T> implements Comparator<T> {

    @SuppressWarnings("unchecked")
    static <C extends Comparable> Ordering<C> natural() {
        return (Ordering<C>) NaturalOrdering.INSTANCE;
    }

    public static <T> Ordering<T> from(Comparator<T> comparator) {
        return (comparator instanceof Ordering) ? (Ordering<T>) comparator : new ComparatorOrdering<>(comparator);
    }

    Ordering() {}

    public <S extends T> Ordering<S> reverse() {
        return new ReverseOrdering<>(this);
    }

    @Override
    public abstract int compare(T left, T right);
}