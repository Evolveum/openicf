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
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Common;
using System.Diagnostics;
using Org.IdentityConnectors.Framework.Common.Exceptions;

namespace Org.IdentityConnectors.ActiveDirectory
{
    /// <summary>
    /// This was taken from the LDAP filter translator (java) and ported to 
    /// C#.  There are a few changes, but not many ... that will change over
    /// time of course.
    /// </summary>
    public class ActiveDirectoryFilterTranslator : AbstractFilterTranslator<String>
    {
        protected override String CreateAndExpression(String leftExpression,
                                             String rightExpression) {          
            StringBuilder builder = new StringBuilder();
            builder.Append("(&");
            builder.Append(leftExpression);
            builder.Append(rightExpression);
            builder.Append(')');
            return builder.ToString();
        }

        protected override String CreateOrExpression(String leftExpression,
                                            String rightExpression) {
            StringBuilder builder = new StringBuilder();
            builder.Append("(|");
            builder.Append(leftExpression);
            builder.Append(rightExpression);
            builder.Append(')');
            return builder.ToString();
        }

        protected override String CreateContainsExpression(ContainsFilter filter,
                                                  Boolean not) {
            String[] attrNames = GetLdapNamesForAttribute(filter.GetAttribute());
            if (attrNames == null) {
                return null;
            }

            StringBuilder builder = new StringBuilder();
            if (not) {
                builder.Append("!(");
            }
            if (attrNames.Length == 1) {
                builder.Append('(');
                builder.Append(attrNames[0]);
                builder.Append("=*");
                int len = builder.Length;
                GetLdapFilterValue(builder, attrNames[0], filter.GetValue());
                // Build (attr=*) rather than (attr=**) for zero-length values.
                if (builder.Length != len) {
                    builder.Append('*');
                }
                builder.Append(')');
            } else {
                builder.Append("(|");
                foreach (String attrName in attrNames) {
                    builder.Append('(');
                    builder.Append(attrName);
                    builder.Append("=*");
                    int len = builder.Length;
                    GetLdapFilterValue(builder, attrName, filter.GetValue());
                    // Build (attr=*) rather than (attr=**) for zero-length values.
                    if (builder.Length != len) {
                        builder.Append('*');
                    }
                    builder.Append(')');
                }
                builder.Append(')');
            }
            if (not) {
                builder.Append(')');
            }
            return builder.ToString();
        }

        protected override String CreateStartsWithExpression(StartsWithFilter filter,
                                                    Boolean not) {
            String[] attrNames = GetLdapNamesForAttribute(filter.GetAttribute());
            if (attrNames == null) {
                return null;
            }

            StringBuilder builder = new StringBuilder();
            if (not) {
                builder.Append("!(");
            }
            if (attrNames.Length == 1) {
                builder.Append('(');
                builder.Append(attrNames[0]);
                builder.Append('=');
                GetLdapFilterValue(builder, attrNames[0], filter.GetValue());
                builder.Append("*)");
            } else {
                builder.Append("(|");
                foreach (String attrName in attrNames) {
                    builder.Append('(');
                    builder.Append(attrName);
                    builder.Append('=');
                    GetLdapFilterValue(builder, attrName, filter.GetValue());
                    builder.Append("*)");
                }
                builder.Append(')');
            }
            if (not) {
                builder.Append(')');
            }
            return builder.ToString();
        }

        protected override String CreateEndsWithExpression(EndsWithFilter filter,
                                                  Boolean not) {
            String[] attrNames = GetLdapNamesForAttribute(filter.GetAttribute());
            if (attrNames == null) {
                return null;
            }

            StringBuilder builder = new StringBuilder();
            if (not) {
                builder.Append("!(");
            }
            if (attrNames.Length == 1) {
                builder.Append('(');
                builder.Append(attrNames[0]);
                builder.Append("=*");
                GetLdapFilterValue(builder, attrNames[0], filter.GetValue());
                builder.Append(')');
            } else {
                builder.Append("(|");
                foreach (String attrName in attrNames) {
                    builder.Append('(');
                    builder.Append(attrName);
                    builder.Append("=*");
                    GetLdapFilterValue(builder, attrName, filter.GetValue());
                    builder.Append(')');
                }
                builder.Append(')');
            }
            if (not) {
                builder.Append(')');
            }
            return builder.ToString();
        }

        protected override String CreateEqualsExpression(EqualsFilter filter, Boolean not) {
            // The LDAP equality filter matches any one attribute value,
            // whereas the connector EqualsFilter matches an attribute and
            // its values exactly.
            if (not) {
                return null;
            }

            ConnectorAttribute attr = filter.GetAttribute();
            // if there is only one thing to search on, and it's
            // a uid we need to convert the uid to something we
            // can search on.  NOTE:  only handling the case where
            // we are doing an equality search, and only one item
            // is in the equality search ... It's all that makes
            // sense for uid.
            if (attr is Uid)
            {
                String attrValue = ((Uid)attr).GetUidValue();
                if (LooksLikeGUID(attrValue))
                {
                    String searchGuid = GetUidSearchString(((Uid)attr).GetUidValue());
                    attr = new Uid(searchGuid);
                } else {
                    attr = new Name(attrValue);
                }
                
            }


            String[] attrNames = GetLdapNamesForAttribute(attr);
            if (attrNames == null) {
                return null;
            }

            StringBuilder builder = new StringBuilder();

            if (attr.Value == null) {
                return null;
            }
            if (attr.Value.Count == 1) {
                BuildEqualityFilter(builder, attrNames,
                                    attr.Value[0]);
            } else {
                builder.Append("(&");
                foreach (Object value in attr.Value) {
                    BuildEqualityFilter(builder, attrNames, value);
                }
                builder.Append(')');
            }

            return builder.ToString();
        }

        protected override String CreateGreaterThanExpression(GreaterThanFilter filter,
                                                     Boolean not) {
            // Note that (!(a > X)) is only the same as (a <= X) if every object
            // has a value of a.
            if (not) {
                return null;
            }

            String[] attrNames = GetLdapNamesForAttribute(filter.GetAttribute());
            if (attrNames == null) {
                return null;
            }

            StringBuilder builder = new StringBuilder();
            BuildGreaterOrEqualFilter(builder, attrNames, filter.GetValue());
            return builder.ToString();
        }

        protected override String CreateGreaterThanOrEqualExpression(
             GreaterThanOrEqualFilter filter, Boolean not) {
            String[] attrNames = GetLdapNamesForAttribute(filter.GetAttribute());
            if (attrNames == null) {
                return null;
            }

            StringBuilder builder = new StringBuilder();
            if (not) {
                builder.Append("!(");
            }
            BuildGreaterOrEqualFilter(builder, attrNames, filter.GetValue());
            if (not) {
                builder.Append(')');
            }
            return builder.ToString();
        }

        protected override String CreateLessThanExpression(LessThanFilter filter,
                                                  Boolean not) {
            // Note that (!(a < X)) is only the same as (a >= X) if every object
            // has a value of a.
            if (not) {
                return null;
            }

            String[] attrNames = GetLdapNamesForAttribute(filter.GetAttribute());
            if (attrNames == null) {
                return null;
            }

            StringBuilder builder = new StringBuilder();
            BuildLessOrEqualFilter(builder, attrNames, filter.GetValue());
            return builder.ToString();
        }

        protected override String CreateLessThanOrEqualExpression(
             LessThanOrEqualFilter filter, Boolean not) {
            String[] attrNames = GetLdapNamesForAttribute(filter.GetAttribute());
            if (attrNames == null) {
                return null;
            }

            StringBuilder builder = new StringBuilder();
            if (not) {
                builder.Append("!(");
            }
            BuildLessOrEqualFilter(builder, attrNames, filter.GetValue());
            if (not) {
                builder.Append(')');
            }
            return builder.ToString();
        }

        /**
         * Get the string representation of an attribute value suitable for
         * embedding in an LDAP search filter (RFC 2254 / RFC 4515).
         *
         * @param builder A string builder on to which a suitably escaped attribute
         *                value will be appended.
         *
         * @param value   The attribute value to be embedded.
         */
        static void GetLdapFilterValue(StringBuilder builder, 
            String AttributeName, Object value) {
            // at this point, this can probably go away
            // it was here to properyly escape queries, but
            // it doesn't seem that they need escaping.
            if (value == null)
            {
                return;
            }
            else
            {
                builder.Append(value);
            }
        }

        /**
         * Get the LDAP name or names for a given connector attribute used in a
         * search filter.
         *
         * @param attr The connector attribute used in a search filter.
         *
         * @return The name or names of the corresponding LDAP attribute.
         *         Returns null if the attribute cannot be specified in an LDAP
         *         filter.
         */

        protected virtual String[] GetLdapNamesForAttribute(ConnectorAttribute attr) {
            // Special processing for certain connector attributes.
            String[] attrNames = null;
            if (attr is Uid) {
                /*
                attrNames = new String[] {
                     configCache.getConfiguration().getUuidAttribute() };
                */
                attrNames = new String[] { "objectGUID" };
            } else if (attr is Name) {
                /*
                attrNames = configCache.getNamingAttributes();
                 */
                attrNames = new String [] { "distinguishedName" };
            } else if (attr.Is(OperationalAttributes.PASSWORD_NAME)) {
                /*
                attrNames = new String[] {
                     configCache.getConfiguration().getPasswordAttribute()
                };
                */
                attrNames = new String[] { "userPassword" };
            } else if (ConnectorAttributeUtil.IsSpecial(attr)) {
                return null;
            } else {
                attrNames = new String[] { attr.Name };
            }

            return attrNames;
        }

        static void BuildEqualityFilter(StringBuilder builder,
                                        String[] attrNames,
                                        Object attrValue) {
            if (attrNames.Length == 1) {
                builder.Append('(');
                builder.Append(attrNames[0]);
                builder.Append('=');
                GetLdapFilterValue(builder, attrNames[0], attrValue);
                builder.Append(')');
            } else {
                builder.Append("(|");
                foreach (String attrName in attrNames) {
                    builder.Append('(');
                    builder.Append(attrName);
                    builder.Append('=');
                    GetLdapFilterValue(builder, attrName, attrValue);
                    builder.Append(')');
                }
                builder.Append(')');
            }
        }

        static void BuildGreaterOrEqualFilter(StringBuilder builder,
                                              String[] attrNames,
                                              Object attrValue) {
            if (attrNames.Length == 1) {
                builder.Append('(');
                builder.Append(attrNames[0]);
                builder.Append(">=");
                GetLdapFilterValue(builder, attrNames[0], attrValue);
                builder.Append(')');
            } else {
                builder.Append("(|");
                foreach (String attrName in attrNames) {
                    builder.Append('(');
                    builder.Append(attrName);
                    builder.Append(">=");
                    GetLdapFilterValue(builder, attrName, attrValue);
                    builder.Append(')');
                }
                builder.Append(')');
            }
        }

        static void BuildLessOrEqualFilter(StringBuilder builder,
                                           String[] attrNames,
                                           Object attrValue) {
            if (attrNames.Length == 1) {
                builder.Append('(');
                builder.Append(attrNames[0]);
                builder.Append("<=");
                GetLdapFilterValue(builder, attrNames[0], attrValue);
                builder.Append(')');
            } else {
                builder.Append("(|");
                foreach (String attrName in attrNames) {
                    builder.Append('(');
                    builder.Append(attrName);
                    builder.Append("<=");
                    GetLdapFilterValue(builder, attrName, attrValue);
                    builder.Append(')');
                }
                builder.Append(')');
            }
        }

        // This is a special case for IDM backward compatibility for 
        // non account objects.  If it doesn't look like a UID, just
        // assume it's a dn
        static internal bool LooksLikeGUID(string value)
        {
            string[] uidStringParts = value.Split('=');
            if ((uidStringParts.Length != 2) || (uidStringParts[1] == null))
            {
                // This is a special case for IDM backward compatibility for 
                // non account objects.  If it doesn't look like a UID, just
                // assume it's a dn
                return false;
            }

            return true;
        }
               
        // This is called to fix up UID values which are in the
        // format <GUID=xxxxx...>, but need to be in the 
        // format \\xx\\xx\\xx...        
        static internal string GetUidSearchString(string uidString)
        {
            // be tolerant of whitespace between '<' and "GUID", 
            // and between "GUID" and '=' and between '=' and
            // start of guidstring, and between start of guidstring
            // and '>'
            string uidSearchString = "";
            string[] uidStringParts = uidString.Split('=');
            if ((uidStringParts.Length != 2) || (uidStringParts[1] == null))
            {
                throw new ConnectorException("Uid is not in the expected format");
            }
            uidSearchString = uidStringParts[1].Trim();
            
            // take off the final '>'
            uidSearchString = uidSearchString.Substring(0, uidSearchString.IndexOf('>'));

            // now put the '\' characters in            
            string escapedSearchString = "";
            for(int position = 0;position < uidSearchString.Length;position++) {                 
                if(position % 2 == 0) {
                    escapedSearchString += "\\";
                }
                escapedSearchString += uidSearchString[position];
            }

            return escapedSearchString;
        }
    }
}
