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

namespace Org.IdentityConnectors.Common
{
    /// <summary>
    /// Description of Assertions.
    /// </summary>
    public static class Assertions
    {
        /// <summary>
        /// Throws <see cref="ArgumentNullException" /> if the parameter <paramref name="o"/>
        /// is <code>null</code>.
        /// </summary>
        /// <param name="o">check if the object is <code>null</code>.</param>
        /// <param name="param">name of the parameter to check for <code>null</code>.</param>
        /// <exception cref="ArgumentNullException">if <paramref name="o"/> is <code>null</code> and constructs a
        /// message with the name of the parameter.</exception>
        public static void NullCheck(Object o, String param)
        {
            String FORMAT = "Parameter '{0}' must not be null.";
            if (o == null)
            {
                throw new ArgumentNullException(String.Format(FORMAT, param));
            }
        }

        /// <summary>
        /// Throws <see cref="ArgumentException" /> if the parameter <paramref name="o"/>
        /// is <code>null</code> or blank.
        /// </summary>
        /// <param name="o">value to test for blank.</param>
        /// <param name="param">name of the parameter to check.</param>
        /// <exception cref="ArgumentException">if <paramref name="o"/> is <code>null</code> or  blank and constructs a
        /// message with the name of the parameter.</exception>
        public static void BlankCheck(String o, String param)
        {
            String FORMAT = "Parameter '{0}' must not be blank.";
            if (StringUtil.IsBlank(o))
            {
                throw new ArgumentException(String.Format(FORMAT, param));
            }
        }
    }
}