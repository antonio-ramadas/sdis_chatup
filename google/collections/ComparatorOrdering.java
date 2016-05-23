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

import java.io.Serializable;
import java.util.Comparator;

final class ComparatorOrdering<T> extends Ordering<T> implements Serializable {

    private final Comparator<T> comparator;

    ComparatorOrdering(final Comparator<T> paramComparator) {
        comparator = paramComparator;
    }

    @Override
    public int compare(T a, T b) {
        return comparator.compare(a, b);
    }

    @Override
    public boolean equals(final Object paramObject) {
        return paramObject == this || paramObject instanceof ComparatorOrdering && comparator.equals(((ComparatorOrdering<?>) paramObject).comparator);
    }

    @Override
    public int hashCode() {
        return comparator.hashCode();
    }

    @Override
    public String toString() {
        return comparator.toString();
    }

    private static final long serialVersionUID = 0;
}