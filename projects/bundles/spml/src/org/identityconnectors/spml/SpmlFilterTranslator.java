/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
package org.identityconnectors.spml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.openspml.v2.profiles.dsml.And;
import org.openspml.v2.profiles.dsml.DSMLProfileException;
import org.openspml.v2.profiles.dsml.DSMLValue;
import org.openspml.v2.profiles.dsml.EqualityMatch;
import org.openspml.v2.profiles.dsml.FilterItem;
import org.openspml.v2.profiles.dsml.GreaterOrEqual;
import org.openspml.v2.profiles.dsml.LessOrEqual;
import org.openspml.v2.profiles.dsml.Or;
import org.openspml.v2.profiles.dsml.Substrings;


public class SpmlFilterTranslator extends AbstractFilterTranslator<FilterItem>{
    private static final ScriptExecutorFactory factory = ScriptExecutorFactory.newInstance("GROOVY");

    private SpmlConnection _connection;
    private SpmlConfiguration _configuration;
    private ScriptExecutor _mapQueryNameExecutor;

    public SpmlFilterTranslator(SpmlConfiguration configuration, SpmlConnection connection) {
        _connection = connection;
        _configuration = configuration;
        try {
            String mapQueryNameCommand = _configuration.getMapQueryNameCommand();
            if (mapQueryNameCommand!=null && mapQueryNameCommand.length()>0)
                _mapQueryNameExecutor = factory.newScriptExecutor(getClass().getClassLoader(), mapQueryNameCommand, true);
        } catch (Exception e) {
            throw new ConnectorException(_configuration.getMessage(SpmlMessages.MAPQUERYNAME_SCRIPT_ERROR), e);
        }
    }

    @Override
    protected FilterItem createAndExpression(FilterItem leftExpression, FilterItem rightExpression) {
        if (leftExpression!=null && rightExpression!=null) {
            And and = new And(new FilterItem[] {leftExpression, rightExpression});
            return and;
        } else if (leftExpression!=null) {
            return leftExpression;
        } else if (rightExpression!=null) {
            return rightExpression;
        } else {
            return super.createAndExpression(leftExpression, rightExpression);
        }
    }

    @Override
    protected FilterItem createOrExpression(FilterItem leftExpression, FilterItem rightExpression) {
        if (leftExpression!=null && rightExpression!=null) {
            Or or = new Or(new FilterItem[] {leftExpression, rightExpression});
            return or;
        } else {
            return super.createOrExpression(leftExpression, rightExpression);
        }
    }

    @Override
    protected FilterItem createStartsWithExpression(StartsWithFilter filter, boolean not) {
        Attribute attribute = filter.getAttribute();
        List<Object> value = attribute.getValue();
        try {
            if (!not && isSingleString(value)) {
                return new Substrings(mapQueryName(attribute.getName()), new DSMLValue((String)value.get(0)), new DSMLValue[0], null);
            } else {
                return super.createStartsWithExpression(filter, not);
            }
        } catch (DSMLProfileException e) {
            throw ConnectorException.wrap(e);
        }
    }

    @Override
    protected FilterItem createContainsExpression(ContainsFilter filter, boolean not) {
        Attribute attribute = filter.getAttribute();
        List<Object> value = attribute.getValue();
        try {
            if (!not && isSingleString(value)) {
                return new Substrings(mapQueryName(attribute.getName()), null, new DSMLValue[] {new DSMLValue((String)value.get(0))}, null);
            } else {
                return super.createContainsExpression(filter, not);
            }
        } catch (DSMLProfileException e) {
            throw ConnectorException.wrap(e);
        }
    }

    @Override
    protected FilterItem createEndsWithExpression(EndsWithFilter filter, boolean not) {
        Attribute attribute = filter.getAttribute();
        List<Object> value = attribute.getValue();
        try {
            if (!not && isSingleString(value)) {
                return new Substrings(mapQueryName(attribute.getName()), null, new DSMLValue[0], new DSMLValue((String)value.get(0)));
            } else {
                return super.createEndsWithExpression(filter, not);
            }
        } catch (DSMLProfileException e) {
            throw ConnectorException.wrap(e);
        }
    }

    @Override
    protected FilterItem createEqualsExpression(EqualsFilter filter, boolean not) {
        Attribute attribute = filter.getAttribute();
        List<Object> value = attribute.getValue();
        try {
            if (!not && isSingleString(value)) {
                return new EqualityMatch(mapQueryName(attribute.getName()), new DSMLValue((String)value.get(0)));
            } else {
                return super.createEqualsExpression(filter, not);
            }
        } catch (DSMLProfileException e) {
            throw ConnectorException.wrap(e);
        }
    }

    @Override
    protected FilterItem createGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, boolean not) {
        Attribute attribute = filter.getAttribute();
        List<Object> value = attribute.getValue();
        try {
            if (!not && isSingleString(value)) {
                GreaterOrEqual goe = new GreaterOrEqual();
                goe.setName(mapQueryName(attribute.getName()));
                goe.setValue(new DSMLValue((String)value.get(0)));
                return goe;
            } else {
                return super.createGreaterThanOrEqualExpression(filter, not);
            }
        } catch (DSMLProfileException e) {
            throw ConnectorException.wrap(e);
        }
    }

    @Override
    protected FilterItem createLessThanOrEqualExpression(LessThanOrEqualFilter filter, boolean not) {
        Attribute attribute = filter.getAttribute();
        List<Object> value = attribute.getValue();
        try {
            if (!not && isSingleString(value)) {
                LessOrEqual loe = new LessOrEqual();
                loe.setName(mapQueryName(attribute.getName()));
                loe.setValue(new DSMLValue((String)value.get(0)));
                return loe;
            } else {
                return super.createLessThanOrEqualExpression(filter, not);
            }
        } catch (DSMLProfileException e) {
            throw ConnectorException.wrap(e);
        }
    }

    private String mapQueryName(String name) {
        if (Uid.NAME.equals(name))
            return SpmlConnector.PSOID;

        try {
            if (_mapQueryNameExecutor!=null) {
                Map<String, Object> arguments = new HashMap<String, Object>();
                arguments.put("name", name);
                arguments.put("configuration", _configuration);
                arguments.put("memory", _connection.getMemory());
                return (String)_mapQueryNameExecutor.execute(arguments);
            }
        } catch (Exception e) {
            throw new ConnectorException(_configuration.getMessage(SpmlMessages.MAPQUERYNAME_SCRIPT_ERROR), e);
        }
        return name;
    }

    private boolean isSingleString(List<Object> value) {
        return value!=null && value.size()==1 && value.get(0) instanceof String;
    }
}
