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
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.webtimesheet;

import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.common.StringUtil;

import org.identityconnectors.common.logging.Log;

/**
 * This is an implementation of AbstractFilterTranslator that gives a concrete representation
 * of which filters can be applied at the connector level (natively). If the 
 * WebTimeSheet doesn't support a certain expression type, that factory
 * method should return null. This level of filtering is present only to allow any
 * native contructs that may be available to help reduce the result set for the framework,
 * which will (strictly) reapply all filters specified after the connector does the initial
 * filtering.<p><p>Note: The generic query type is most commonly a String, but does not have to be.
 * 
 * @author Robert Jackson - <a href='http://www.nulli.com'>Nulli Secundus Inc.</a>
 * @version 1.0
 * @since 1.0
 */
public class WebTimeSheetFilterTranslator extends AbstractFilterTranslator<String> {

    private static final Log log = Log.getLog(WebTimeSheetFilterTranslator.class);

    /**
     * {@inheritDoc}
     */
    /*    @Override
    protected String createContainsExpression(ContainsFilter filter, boolean not) {
    return null;
    }

    /**
     * {@inheritDoc}
     */
    /*    @Override
    protected String createEndsWithExpression(EndsWithFilter filter, boolean not) {
    return null;
    }

    /**
     * {@inheritDoc}
     */
    /*    @Override
    protected String createStartsWithExpression(StartsWithFilter filter, boolean not) {
    return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createEqualsExpression(EqualsFilter filter, boolean not) {
        String name = filter.getAttribute().getName();
        if ((name.equals("__NAME__")) || (name.equals("__UID__"))) {
            name = WebTimeSheetConnector.ATTR_ID;
        }
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            //default search is "Id not null"
            log.info("filter: " + "<id operator='notequal'></id>");
            return "<id operator='notequal'></id>";
        } else if (not) {
            log.info("filter: " + "<" + name + " operator='notequal'>" + value + "</" + name + ">");
            return "<" + name + " operator='notequal'>" + value + "</" + name + ">";
        } else {
            log.info("filter: " + "<" + name + " operator='equal'>" + value + "</" + name + ">");
            return "<" + name + " operator='equal'>" + value + "</" + name + ">";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createAndExpression(String leftExpression, String rightExpression) {
        return "<And>" + leftExpression + rightExpression + "</And>";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createOrExpression(String leftExpression, String rightExpression) {
        return "<Or>" + leftExpression + rightExpression + "</Or>";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createGreaterThanExpression(GreaterThanFilter filter, boolean not) {
        String name = filter.getAttribute().getName();
        if ((name.equals("__NAME__")) || (name.equals("__UID__"))) {
            name = WebTimeSheetConnector.ATTR_ID;
        }
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        } else if (not) {
            return "<Not><" + name + " operator='greaterthan'>" + value + "</" + name + "></Not>";
        } else {
            return "<" + name + " operator='greaterthan'>" + value + "</" + name + ">";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, boolean not) {
        String name = filter.getAttribute().getName();
        if ((name.equals("__NAME__")) || (name.equals("__UID__"))) {
            name = WebTimeSheetConnector.ATTR_ID;
        }
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        } else if (not) {
            return "<Not><" + name + " operator='greaterorequal'>" + value + "</" + name + "></Not>";
        } else {
            return "<" + name + " operator='greaterorequal'>" + value + "</" + name + ">";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createLessThanExpression(LessThanFilter filter, boolean not) {
        String name = filter.getAttribute().getName();
        if ((name.equals("__NAME__")) || (name.equals("__UID__"))) {
            name = WebTimeSheetConnector.ATTR_ID;
        }
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        } else if (not) {
            return "<Not><" + name + " operator='lessthan'>" + value + "</" + name + "></Not>";
        } else {
            return "<" + name + " operator='lessthan'>" + value + "</" + name + ">";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createLessThanOrEqualExpression(LessThanOrEqualFilter filter, boolean not) {
        String name = filter.getAttribute().getName();
        if ((name.equals("__NAME__")) || (name.equals("__UID__"))) {
            name = WebTimeSheetConnector.ATTR_ID;
        }
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        } else if (not) {
            return "<Not><" + name + " operator='lessorequal'>" + value + "</" + name + "></Not>";
        } else {
            return "<" + name + " operator='lessorequal'>" + value + "</" + name + ">";
        }
    }
}
