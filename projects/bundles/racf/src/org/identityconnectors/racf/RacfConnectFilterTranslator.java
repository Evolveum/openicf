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
package org.identityconnectors.racf;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;


/**
 * Create LDAP search Filter
 * <p>
 * Supported filters for "profileType=connect,suffixDN" are:
 * <ul>
 *		<li>objectclass=*
 *		<li>racfuserid=&lt;any_value&gt;</li>
 *		<li>racfgroupid=&lt;any_value&gt;</li>
 *		<li>((&(racfuserid=&lt;any_value&gt;)(racfgroupid=&lt;any_value&gt;))</li>
 * </ul>
 * <p>
 * Except for the AND filter for connections, complex search filters that 
 * include NOT, AND, OR, LE, or GE constructs are not supported.
 * <p>
 * The values for the racfid, racfuserid, and racfgroupid filters can include 
 * the wild cards supported by RACF. These wild cards are '*' which represents 
 * any number of characters, and '%' which represents one character. For example:
 * <pre>
 * (&(racfuserid=usr*)(racfgroupid=*grp))
 * </pre> 
 * searches for all the connections between users whose names begin with usr 
 * and groups whose names end with grp.
 */
public class RacfConnectFilterTranslator extends AbstractFilterTranslator<String> {
    @Override
    protected String createAndExpression(String leftExpression, String rightExpression) {
        // The only AND supported is 'racfuserid' and 'racfgroupid'
        //
        if (leftExpression!=null && rightExpression!=null) {
            boolean isRacfuserid = leftExpression.startsWith("(racfuserid=") || rightExpression.startsWith("(racfuserid=");
            boolean isRacfgroupid = leftExpression.startsWith("(racfgroupid=") || rightExpression.startsWith("(racfgroupid=");
            if (isRacfuserid && isRacfgroupid) 
                return "(&"+leftExpression+rightExpression+")";
        }

        if (leftExpression!=null)
            return leftExpression;
        else if (rightExpression!=null)
            return rightExpression;
        else
            return super.createAndExpression(leftExpression, rightExpression);
    }
    @Override
    protected String createStartsWithExpression(StartsWithFilter filter, boolean not) {
        if (!not && isUserFilterAttribute(filter.getAttribute()))
            return "("+filter.getAttribute()+"="+AttributeUtil.getAsStringValue(filter.getAttribute())+"*)";
        else
            return super.createStartsWithExpression(filter, not);
    }

    @Override
    protected String createEqualsExpression(EqualsFilter filter, boolean not) {
        if (!not && isUserFilterAttribute(filter.getAttribute()))
            return "("+filter.getAttribute()+"="+AttributeUtil.getAsStringValue(filter.getAttribute())+")";
        else
            return super.createEqualsExpression(filter, not);
    }

    @Override
    protected String createEndsWithExpression(EndsWithFilter filter, boolean not) {
        if (!not && isUserFilterAttribute(filter.getAttribute()))
            return "("+filter.getAttribute()+"=*"+AttributeUtil.getAsStringValue(filter.getAttribute())+")";
        else
            return super.createEndsWithExpression(filter, not);
    }

    @Override
    protected String createContainsExpression(ContainsFilter filter, boolean not) {
        if (!not && isUserFilterAttribute(filter.getAttribute()))
            return "("+filter.getAttribute()+"=*"+AttributeUtil.getAsStringValue(filter.getAttribute())+"*)";
        else
            return super.createContainsExpression(filter, not);
    }
    
    private boolean isUserFilterAttribute(Attribute attribute) {
        if (attribute.is(RacfConstants.RACF_USERID))
            return true;
        if (attribute.is(RacfConstants.RACF_GROUPID))
            return true;
        return false;
    }
}
