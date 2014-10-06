/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
 */

using System;
using System.Collections.Generic;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Common;

namespace Org.ForgeRock.OpenICF.Connectors.MsPowerShell
{
    public class MsPowerShellFilterTranslator : AbstractFilterTranslator<IDictionary<String,Object>>
    {
        // The Query map describes the filter used.
        //
        // query = [ operation: "CONTAINS", left: attribute, right: "value", not: true/false ]
        // query = [ operation: "ENDSWITH", left: attribute, right: "value", not: true/false ]
        // query = [ operation: "STARTSWITH", left: attribute, right: "value", not: true/false ]
        // query = [ operation: "EQUALS", left: attribute, right: "value", not: true/false ]
        // query = [ operation: "GREATERTHAN", left: attribute, right: "value", not: true/false ]
        // query = [ operation: "GREATERTHANOREQUAL", left: attribute, right: "value", not: true/false ]
        // query = [ operation: "LESSTHAN", left: attribute, right: "value", not: true/false ]
        // query = [ operation: "LESSTHANOREQUAL", left: attribute, right: "value", not: true/false ]
        // query = null : then we assume we fetch everything
        //
        // AND and OR filter just embed a left/right couple of queries.
        // query = [ operation: "AND", left: query1, right: query2 ]
        // query = [ operation: "OR", left: query1, right: query2 ]
        //

        private const String Not = "Not";
        private const String Operation = "Operation";
        private const String Left = "Left";
        private const String Right = "Right";
        private const String And = "And";
        private const String Or = "Or";

        private static IDictionary<String, Object> CreateFilter(String operation, AttributeFilter filter, bool not)
        {
            IDictionary<String, Object> dic = new Dictionary<String, Object>();
            var name = filter.GetAttribute().Name;
            var value = ConnectorAttributeUtil.GetStringValue(filter.GetAttribute());
            if (StringUtil.IsBlank(value))
            {
                return null;
            }
            dic.Add(Not, not);
            dic.Add(Operation, operation);
            dic.Add(Left, name);
            dic.Add(Right, value);
            return dic;
        }

        protected override IDictionary<String, Object> CreateAndExpression(IDictionary<String, Object> leftExpression, IDictionary<String, Object> rightExpression) 
        {
            IDictionary<String,Object> dic = new Dictionary<String,Object>();
            dic.Add(Operation, And);
            dic.Add(Left, leftExpression);
            dic.Add(Right, rightExpression);
            return dic;
        }

        protected override IDictionary<String, Object> CreateOrExpression(IDictionary<String, Object> leftExpression, IDictionary<String, Object> rightExpression) 
        {
            IDictionary<String,Object> dic = new Dictionary<String,Object>();
            dic.Add(Operation, Or);
            dic.Add(Left, leftExpression);
            dic.Add(Right, rightExpression);
            return dic;
        }

        protected override IDictionary<String, Object> CreateContainsExpression(ContainsFilter filter, Boolean not) 
        {
            return CreateFilter("CONTAINS", filter, not);
        }

        protected override IDictionary<String, Object> CreateStartsWithExpression(StartsWithFilter filter, Boolean not) 
        {
            return CreateFilter("STARTSWITH", filter, not);
        }

        protected override IDictionary<String, Object> CreateEndsWithExpression(EndsWithFilter filter, Boolean not) 
        {
            return CreateFilter("ENDSWITH", filter, not);
        }

        protected override IDictionary<String, Object> CreateEqualsExpression(EqualsFilter filter, Boolean not) 
        {
            return CreateFilter("EQUALS", filter, not);
        }

        protected override IDictionary<String, Object> CreateGreaterThanExpression(GreaterThanFilter filter, Boolean not) 
        {
            return CreateFilter("GREATERTHAN", filter, not);
        }

        protected override IDictionary<String, Object> CreateGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, Boolean not) 
        {
            return CreateFilter("GREATERTHANOREQUAL", filter, not);
        }

        protected override IDictionary<String, Object> CreateLessThanExpression(LessThanFilter filter, Boolean not) 
        {
            return CreateFilter("LESSTHAN", filter, not);
        }

        protected override IDictionary<String, Object> CreateLessThanOrEqualExpression(LessThanOrEqualFilter filter, Boolean not) 
        {
            return CreateFilter("LESSTHANOREQUAL", filter, not);
        }
    }

    public class MsPowerShellFilterTranslator2 : AbstractFilterTranslator<Filter>
    {

        public static readonly AbstractFilterTranslator<Filter> Instance = new MsPowerShellFilterTranslator2();

        protected override Filter CreateAndExpression(Filter leftExpression, Filter rightExpression)
        {
            return FilterBuilder.And(leftExpression, rightExpression);
        }

        protected override Filter CreateContainsAllValuesExpression(ContainsAllValuesFilter filter, bool not)
        {
            return not ? FilterBuilder.Not(filter) : filter;
        }

        protected override Filter CreateContainsExpression(ContainsFilter filter, bool not)
        {
            return not ? FilterBuilder.Not(filter) : filter;
        }

        protected override Filter CreateEndsWithExpression(EndsWithFilter filter, bool not)
        {
            return not ? FilterBuilder.Not(filter) : filter;
        }

        protected override Filter CreateEqualsExpression(EqualsFilter filter, bool not)
        {
            return not ? FilterBuilder.Not(filter) : filter;
        }

        protected override Filter CreateGreaterThanExpression(GreaterThanFilter filter, bool not)
        {
            return not ? FilterBuilder.Not(filter) : filter;
        }

        protected override Filter CreateGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, bool not)
        {
            return not ? FilterBuilder.Not(filter) : filter;
        }

        protected override Filter CreateLessThanExpression(LessThanFilter filter, bool not)
        {
            return not ? FilterBuilder.Not(filter) : filter;
        }

        protected override Filter CreateLessThanOrEqualExpression(LessThanOrEqualFilter filter, bool not)
        {
            return not ? FilterBuilder.Not(filter) : filter;
        }

        protected override Filter CreateOrExpression(Filter leftExpression, Filter rightExpression)
        {
            return FilterBuilder.Or(leftExpression, rightExpression);
        }

        protected override Filter CreateStartsWithExpression(StartsWithFilter filter, bool not)
        {
            return not ? FilterBuilder.Not(filter) : filter;
        }
    }

}
