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
using System;
using System.Text;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Exceptions;

namespace Org.IdentityConnectors.Exchange
{
    public class IdentityFilterTranslator : AbstractFilterTranslator<String>
    {
        protected override String CreateAndExpression(String leftExpression,
                                             String rightExpression) {
            if (leftExpression != null) {
                return leftExpression;
            } else {
                return rightExpression;
            }
        }

        protected override String CreateEqualsExpression(EqualsFilter filter, Boolean not) {
            return CreateContainsAllValuesExpressionInternal(filter, not);
        }

        protected override String CreateContainsAllValuesExpression(ContainsAllValuesFilter filter, Boolean not) {
            return CreateContainsAllValuesExpressionInternal(filter, not);
        }

        private String CreateContainsAllValuesExpressionInternal(AttributeFilter filter, Boolean not) 
        {
            if (not) {
                return null;
            }

            ConnectorAttribute attr = filter.GetAttribute();
            if (attr is Uid) {
                return ((Uid)attr).GetUidValue();
            } else if (attr is Name) {
                return ((Name)attr).GetNameValue();
            } else {
                return null;
            }
        }
    }
}
