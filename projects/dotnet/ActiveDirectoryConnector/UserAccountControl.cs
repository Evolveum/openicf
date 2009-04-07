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
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.DirectoryServices;
using Org.IdentityConnectors.Framework.Common.Exceptions;

namespace Org.IdentityConnectors.ActiveDirectory
{
    internal class UserAccountControl
    {
        public static string UAC_ATTRIBUTE_NAME = "userAccountControl";

        // values taken from - http://support.microsoft.com/kb/305144
        static public int SCRIPT = 0x0001;
        static public int ACCOUNTDISABLE = 0x0002;
        static public int HOMEDIR_REQUIRED = 0x0008;
        static public int LOCKOUT = 0x0010;
        static public int PASSWD_NOTREQD = 0x0020;

        // Note You cannot assign this permission by directly modifying 
        // the UserAccountControl attribute. For information about how 
        // to set the permission programmatically, see the "Property 
        // flag descriptions" section.
        static public int PASSWD_CANT_CHANGE = 0x0040;

        static public int ENCRYPTED_TEXT_PWD_ALLOWED = 0x0080;
        static public int TEMP_DUPLICATE_ACCOUNT = 0x0100;
        static public int NORMAL_ACCOUNT = 0x0200;
        static public int INTERDOMAIN_TRUST_ACCOUNT = 0x0800;
        static public int WORKSTATION_TRUST_ACCOUNT = 0x1000;
        static public int SERVER_TRUST_ACCOUNT = 0x2000;
        static public int DONT_EXPIRE_PASSWORD = 0x10000;
        static public int MNS_LOGON_ACCOUNT = 0x20000;
        static public int SMARTCARD_REQUIRED = 0x40000;
        static public int TRUSTED_FOR_DELEGATION = 0x80000;
        static public int NOT_DELEGATED = 0x100000;
        static public int USE_DES_KEY_ONLY = 0x200000;
        static public int DONT_REQ_PREAUTH = 0x400000;
        public static int PASSWORD_EXPIRED = 0x800000;
        public static int TRUSTED_TO_AUTH_FOR_DELEGATION = 0x1000000;

        // get the uac value from the property value collection
        private static int GetUAC(PropertyValueCollection pvc)
        {
            // default schema says it's an integer, so it better be one
            if ((pvc != null) && (pvc.Count == 1) && (pvc[0] is int))
            {
                return (int)pvc[0];
            }
            else
            {
                return 0;
            }
        }

        // sets the uac value in a propertyvaluecollection
        private static void SetUAC(PropertyValueCollection pvc, int value)
        {
            // set the value
            pvc[0] = value;
        }

        // generically set a value in the uac to the value of 'isSet'
        internal static void Set(PropertyValueCollection pvc, int flag, bool? isSet)
        {           
            int uac = GetUAC(pvc);
            if(isSet == null)
            {
                throw new ArgumentException();
            }
            // boolean false
            if (isSet.Value.Equals(false))
            {
                uac &= (~flag);
            }
            else
            {
                uac |= flag;
            }
            SetUAC(pvc, uac);
        }

        // chec to see if a particular value of the uac is set
        internal static bool IsSet(PropertyValueCollection pvc, int flag)
        {
            int uac = GetUAC(pvc);
            return ((uac & flag) != 0);
        }
    }
}
