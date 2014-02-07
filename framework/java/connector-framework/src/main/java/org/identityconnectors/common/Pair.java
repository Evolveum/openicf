/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 * Portions Copyrighted 2013 ConnId
 * Portions Copyrighted 2010-2013 ForgeRock AS.
 */
package org.identityconnectors.common;

import java.util.Map;

/**
 * An arbitrary pair of objects. Convenient implementation of Map.Entry.
 */
public class Pair<T1, T2> implements Map.Entry<T1, T2> {

    public T1 first;

    public T2 second;

    public Pair() {
    }

    /**
     * Obtains a mutable pair of two objects inferring the generic
     * types.
     *
     * <p>
     * This factory allows the pair to be created using inference to obtain the
     * generic types.
     * </p>
     *
     * @param <L>
     *            the left element type
     * @param <R>
     *            the right element type
     * @param left
     *            the left element, may be null
     * @param right
     *            the right element, may be null
     * @return a pair formed from the two parameters, not null
     * @since 1.4
     */
    public static <L, R> Pair<L, R> of(final L left, final R right) {
        return new Pair<L, R>(left, right);
    }

    public Pair(final T1 f, final T2 s) {
        this.first = f;
        this.second = s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int rv = 0;
        if (first != null) {
            rv ^= first.hashCode();
        }
        if (second != null) {
            rv ^= second.hashCode();
        }
        return rv;
    }

    private boolean equals(final Object o1, final Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else if (o2 == null) {
            return false;
        } else {
            return o1.equals(o2);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof Pair) {
            final Pair<?, ?> other = (Pair<?, ?>) object;
            return (equals(this.first, other.first) && equals(this.second, other.second));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "( " + this.first + ", " + this.second + " )";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T1 getKey() {
        return this.first;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T2 getValue() {
        return this.second;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T2 setValue(final T2 value) {
        this.second = value;
        return this.second;
    }
}
