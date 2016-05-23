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

final class ReverseNaturalOrdering extends Ordering<Comparable> implements Serializable {

    static final ReverseNaturalOrdering INSTANCE = new ReverseNaturalOrdering();

    @Override
    @SuppressWarnings("unchecked")
    public int compare(final Comparable lhsComparable, final Comparable rhsComparable) {

        if (lhsComparable == rhsComparable) {
            return 0;
        }

        return rhsComparable.compareTo(lhsComparable);
    }

    @Override
    public <S extends Comparable> Ordering<S> reverse() {
        return Ordering.natural();
    }

    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "Ordering.natural().reverse()";
    }

    private ReverseNaturalOrdering() {}

    private static final long serialVersionUID = 0;
}