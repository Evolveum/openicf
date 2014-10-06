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
using System.Collections;
using System.Collections.Generic;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;

namespace Org.ForgeRock.OpenICF.Connectors.MsPowerShell
{
    public class MapFilterVisitor : FilterVisitor<Dictionary<String, Object>, Hashtable>
    {
        private const String Not = "Not";
        private const String Operation = "Operation";
        private const String Left = "Left";
        private const String Right = "Right";
        private const String And = "And";
        private const String Or = "Or";

        public Dictionary<string, object> VisitAndFilter(Hashtable p, AndFilter filter)
        {
            var dic = new Dictionary<String, Object>
            {
                {Not, false},
                {Left, filter.Left.Accept<Dictionary<String, Object>, Hashtable>(this, p)},
                {Right, filter.Right.Accept<Dictionary<String, Object>, Hashtable>(this, p)},
                {Operation, And}
            };
            return dic;
        }

        public Dictionary<string, object> VisitContainsFilter(Hashtable p, ContainsFilter filter)
        {
            return CreateMap("CONTAINS", filter.GetName(), filter.GetValue());
        }

        public Dictionary<string, object> VisitContainsAllValuesFilter(Hashtable p, ContainsAllValuesFilter filter)
        {
            throw new NotImplementedException();
        }

        public Dictionary<string, object> VisitEqualsFilter(Hashtable p, EqualsFilter filter)
        {
            var values = filter.GetAttribute().Value;
            if (values.Count == 1)
            {
                return CreateMap("EQUALS", filter.GetAttribute().Name, values[0]);
            }
            throw new NotImplementedException("Equality visitor does not implement multi value attribute");
        }

        public Dictionary<string, object> VisitExtendedFilter(Hashtable p, Filter filter)
        {
            throw new NotImplementedException();
        }

        public Dictionary<string, object> VisitGreaterThanFilter(Hashtable p, GreaterThanFilter filter)
        {
            return CreateMap("GREATERTHAN", filter.GetName(), filter.GetValue());
        }

        public Dictionary<string, object> VisitGreaterThanOrEqualFilter(Hashtable p, GreaterThanOrEqualFilter filter)
        {
            return CreateMap("GREATERTHANOREQUAL", filter.GetName(), filter.GetValue());
        }

        public Dictionary<string, object> VisitLessThanFilter(Hashtable p, LessThanFilter filter)
        {
            return CreateMap("LESSTHAN", filter.GetName(), filter.GetValue());
        }

        public Dictionary<string, object> VisitLessThanOrEqualFilter(Hashtable p, LessThanOrEqualFilter filter)
        {
            return CreateMap("LESSTHANOREQUAL", filter.GetName(), filter.GetValue());
        }

        public Dictionary<string, object> VisitNotFilter(Hashtable p, NotFilter filter)
        {
            var dic = filter.Accept<Dictionary<String, Object>, Hashtable>(this, p);
            dic[Not] = true;
            return dic;
        }

        public Dictionary<string, object> VisitOrFilter(Hashtable p, OrFilter filter)
        {
            var dic = new Dictionary<String, Object>
            {
                {Not, false},
                {Left, filter.Left.Accept<Dictionary<String, Object>, Hashtable>(this, p)},
                {Right, filter.Right.Accept<Dictionary<String, Object>, Hashtable>(this, p)},
                {Operation, Or}
            };
            return dic;
        }

        public Dictionary<string, object> VisitStartsWithFilter(Hashtable p, StartsWithFilter filter)
        {
            return CreateMap("STARTSWITH", filter.GetName(), filter.GetValue());
        }

        public Dictionary<string, object> VisitEndsWithFilter(Hashtable p, EndsWithFilter filter)
        {
            return CreateMap("ENDSWITH", filter.GetName(), filter.GetValue());
        }

        private static Dictionary<String, Object> CreateMap(String operation, String name, Object value)
        {
            var dic = new Dictionary<String, Object>();
            if (value == null)
            {
                return null;
            }
            dic.Add(Not, false);
            dic.Add(Operation, operation);
            dic.Add(Left, name);
            dic.Add(Right, value);
            return dic;
        }
    }
}
