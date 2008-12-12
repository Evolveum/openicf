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
using System.Reflection;
using System.IO;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Serializer;

namespace Org.IdentityConnectors.ActiveDirectory
{
    public class CommonUtils
    {
        ///<summary>
        /// reads the object class info definitions from xml
        ///</summary>
        ///<returns>Dictionary of object classes</returns>
        protected internal static IDictionary<ObjectClass, ObjectClassInfo> GetOCInfo(string name)
        {
            Assembly assembly = Assembly.GetExecutingAssembly();
            Stream stream = assembly.GetManifestResourceStream(name);

            Assertions.NullCheck(stream, "stream");

            //we just read
            TextReader streamReader = new StreamReader(stream);
            String xml;
            try
            {
                xml = streamReader.ReadToEnd();
            }
            finally
            {
                streamReader.Close();
            }

            //read from xml
            var ret = (ICollection<object>)SerializerUtil.DeserializeXmlObject(xml, true);

            Assertions.NullCheck(ret, "ret");

            //create map of object infos
            var map = new Dictionary<ObjectClass, ObjectClassInfo>(ret.Count);
            foreach (ObjectClassInfo o in ret)
            {
                map.Add(new ObjectClass(o.ObjectType.ToString()), o);
            }

            return map;
        }
    }
}
