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

final class ReverseOrdering<T> extends Ordering<T> implements Serializable {

    private final Ordering<? super T> forwardOrder;

    ReverseOrdering(Ordering<? super T> paramOrder) {
        forwardOrder = paramOrder;
    }

    @Override
    public int compare(T a, T b) {
        return forwardOrder.compare(b, a);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends T> Ordering<S> reverse() {
        return (Ordering<S>) forwardOrder;
    }

    @Override
    public int hashCode() {
        return -forwardOrder.hashCode();
    }

    @Override
    public boolean equals(final Object paramObject) {
        return paramObject == this || paramObject instanceof ReverseOrdering && forwardOrder.equals(((ReverseOrdering<?>) paramObject).forwardOrder);
    }

    @Override
    public String toString() {
        return forwardOrder + ".reverse()";
    }

    private static final long serialVersionUID = 0;
}