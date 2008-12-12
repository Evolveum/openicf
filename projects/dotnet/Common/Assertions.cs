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

namespace Org.IdentityConnectors.Common
{
    /// <summary>
    /// Description of Assertions.
    /// </summary>
    public static class Assertions
    {
        /**
         * Throws {@link NullPointerException} if the parameter <code>o</code>
         * is <code>null</code>.
         * 
         * @param o
         *            check if the object is <code>null</code>.
         * @param param
         *            name of the parameter to check for <code>null</code>.
         * @throws NullPointerException
         *             if <code>o</code> is <code>null</code> and constructs a
         *             message with the name of the parameter.
         */
        public static void NullCheck(Object o, String param) {
            String FORMAT = "Parameter '{0}' must not be null.";
            if (o == null) {
                throw new ArgumentNullException(String.Format(FORMAT, param));
            }
        }
    }
}
