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
    class LdapFilterVisitor : FilterVisitor<String, Hashtable>
    {
        public string VisitAndFilter(Hashtable p, AndFilter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitContainsFilter(Hashtable p, ContainsFilter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitContainsAllValuesFilter(Hashtable p, ContainsAllValuesFilter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitEqualsFilter(Hashtable p, EqualsFilter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitExtendedFilter(Hashtable p, Filter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitGreaterThanFilter(Hashtable p, GreaterThanFilter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitGreaterThanOrEqualFilter(Hashtable p, GreaterThanOrEqualFilter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitLessThanFilter(Hashtable p, LessThanFilter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitLessThanOrEqualFilter(Hashtable p, LessThanOrEqualFilter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitNotFilter(Hashtable p, NotFilter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitOrFilter(Hashtable p, OrFilter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitStartsWithFilter(Hashtable p, StartsWithFilter filter)
        {
            throw new NotImplementedException();
        }

        public string VisitEndsWithFilter(Hashtable p, EndsWithFilter filter)
        {
            throw new NotImplementedException();
        }
    }
}
