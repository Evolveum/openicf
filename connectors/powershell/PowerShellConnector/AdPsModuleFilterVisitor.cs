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
using Org.IdentityConnectors.Framework.Common.Objects.Filters;

namespace Org.ForgeRock.OpenICF.Connectors.MsPowerShell
{
    class AdPsModuleFilterVisitor : FilterVisitor<String, Hashtable>
    {
        /// <summary>
        /// AndFilter
        /// </summary>
        /// <param name="p"></param>
        /// <param name="filter"></param>
        /// <returns></returns>
        public string VisitAndFilter(Hashtable p, AndFilter filter)
        {
            var l = filter.Left.Accept<String, Hashtable>(this, p);
            var r = filter.Right.Accept<String, Hashtable>(this, p);
            return string.Format("{0} -and {1}", l, r);
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="p"></param>
        /// <param name="filter"></param>
        /// <returns></returns>
        public string VisitContainsFilter(Hashtable p, ContainsFilter filter)
        {
            var name = filter.GetName();
            var name2 = name as String;
            if (p.ContainsKey(name))
            {
                name2 = p[name] as String;
            }
            return string.Format("{0} -like \"{1}{2}{3}\"", name2, "*", filter.GetValue(), "*");
        }

        public string VisitContainsAllValuesFilter(Hashtable p, ContainsAllValuesFilter filter)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="p"></param>
        /// <param name="filter"></param>
        /// <exception cref="NotImplementedException"></exception>
        /// <returns></returns>
        public string VisitEqualsFilter(Hashtable p, EqualsFilter filter)
        {
            var name = filter.GetAttribute().Name;
            var name2 = name as String;
            if (p.ContainsKey(name))
            {
                name2 = p[name] as String;
            }
            var values = filter.GetAttribute().Value;
            if (values.Count == 1)
            {
                return string.Format("{0} -eq \"{1}\"", name2, values[0]);
            }
            throw new NotImplementedException("Equality visitor does not implement multi value attributes");
        }

        public string VisitExtendedFilter(Hashtable p, Filter filter)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="p"></param>
        /// <param name="filter"></param>
        /// <returns></returns>
        public string VisitGreaterThanFilter(Hashtable p, GreaterThanFilter filter)
        {
            var name = filter.GetName();
            var name2 = name as String;
            if (p.ContainsKey(name))
            {
                name2 = p[name] as String;
            }
            return string.Format("{0} -gt {1}", name2, filter.GetValue());
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="p"></param>
        /// <param name="filter"></param>
        /// <returns></returns>
        public string VisitGreaterThanOrEqualFilter(Hashtable p, GreaterThanOrEqualFilter filter)
        {
            var name = filter.GetName();
            var name2 = name as String;
            if (p.ContainsKey(name))
            {
                name2 = p[name] as String;
            }
            return string.Format("{0} -ge {1}", name2, filter.GetValue());
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="p"></param>
        /// <param name="filter"></param>
        /// <returns></returns>
        public string VisitLessThanFilter(Hashtable p, LessThanFilter filter)
        {
            var name = filter.GetName();
            var name2 = name as String;
            if (p.ContainsKey(name))
            {
                name2 = p[name] as String;
            }
            return string.Format("{0} -lt {1}", name2, filter.GetValue());
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="p"></param>
        /// <param name="filter"></param>
        /// <returns></returns>
        public string VisitLessThanOrEqualFilter(Hashtable p, LessThanOrEqualFilter filter)
        {
            var name = filter.GetName();
            var name2 = name as String;
            if (p.ContainsKey(name))
            {
                name2 = p[name] as String;
            }
            return string.Format("{0} -le {1}", name2, filter.GetValue());
        }

        public string VisitNotFilter(Hashtable p, NotFilter filter)
        {
            return string.Format("-not {0}", filter.Filter.Accept(this, p));
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="p"></param>
        /// <param name="filter"></param>
        /// <returns></returns>
        public string VisitOrFilter(Hashtable p, OrFilter filter)
        {
            var l = filter.Left.Accept<String, Hashtable>(this, p);
            var r = filter.Right.Accept<String, Hashtable>(this, p);
            return string.Format("{0} -or {1}", l, r);
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="p"></param>
        /// <param name="filter"></param>
        /// <returns></returns>
        public string VisitStartsWithFilter(Hashtable p, StartsWithFilter filter)
        {
            var name = filter.GetName();
            var name2 = name as String;
            if (p.ContainsKey(name))
            {
                name2 = p[name] as String;
            }
            return string.Format("{0} -like \"{1}{2}\"", name2, filter.GetValue(), "*");
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="p"></param>
        /// <param name="filter"></param>
        /// <returns></returns>
        public string VisitEndsWithFilter(Hashtable p, EndsWithFilter filter)
        {
            var name = filter.GetName();
            var name2 = name as String;
            if (p.ContainsKey(name))
            {
                name2 = p[name] as String;
            }
            return string.Format("{0} -like \"{1}{2}\"", name2, "*", filter.GetValue());
        }
    }
}
