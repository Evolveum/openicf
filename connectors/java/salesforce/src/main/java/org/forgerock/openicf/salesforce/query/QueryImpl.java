/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * $Id$
 */
package org.forgerock.openicf.salesforce.query;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.forgerock.openicf.salesforce.annotation.ICResource;

public class QueryImpl implements IQuery {

    static final IPart RIGHT = new SimplePart("(");
    static final IPart LEFT = new SimplePart(")");
    static final IPart AND = new SimplePart(" AND ");
    static final IPart OR = new SimplePart(" OR ");

    private ICResource resource;
    private IQueryPart mainPart;
    private LinkedList<IPart> parts;

    public QueryImpl(ICResource resource) {
        this.resource = resource;
        this.parts = new LinkedList<IPart>();
    }

    /**
     * {@inheritDoc}
     */
    public void set(IQueryPart part) {
        if (mainPart != null && parts.contains(mainPart)) {
            int index = parts.indexOf(mainPart);
            parts.remove(mainPart);
            parts.add(index, part);
        } else {
            parts.add(part);
        }

        // keep pointer to current part
        mainPart = part;
    }

    /**
     * {@inheritDoc}
     */
    public void and(IQuery part) {
        parts.addFirst(RIGHT);
        parts.addLast(AND);

        for (IPart p : part.getParts()) {
            parts.addLast(p);
        }
        parts.addLast(LEFT);
    }

    /**
     * {@inheritDoc}
     */
    public Collection<IPart> getParts() {
        return parts;
    }

    /**
     * {@inheritDoc}
     */
    public ICResource getResource() {
        return resource;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<IPart> iterator() {
        return parts.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public void or(IQuery part) {
        parts.addFirst(RIGHT);
        parts.addLast(OR);

        for (IPart p : part.getParts()) {
            parts.addLast(p);
        }
        parts.addLast(LEFT);
    }

    static class SimplePart implements IPart {

        private String value;

        public SimplePart(String value) {
            this.value = value;
        }

        /**
         * {@inheritDoc}
         */
        public String getOperator() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return getOperator();
        }

    }

}
