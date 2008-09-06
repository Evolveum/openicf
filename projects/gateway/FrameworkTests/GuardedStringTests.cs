/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
using System;
using System.Security;
using System.Runtime.InteropServices;
using System.Text;
using Org.IdentityConnectors.Common.Security;
using Org.IdentityConnectors.Framework.Common.Serializer;
using NUnit.Framework;
using NUnit.Framework.SyntaxHelpers;

namespace FrameworkTests
{
    [TestFixture]
    public class GuardedStringTests
    {
        [Test]
        public void TestBasics()
        {
            GuardedString ss = new GuardedString();
            ss.AppendChar('f');
            ss.AppendChar('o');
            ss.AppendChar('o');
            ss.AppendChar('b');
            ss.AppendChar('a');
            ss.AppendChar('r');
            String decrypted = DecryptToString(ss);
            Assert.AreEqual("foobar",decrypted);
            String hash = ss.GetBase64SHA1Hash();
            Assert.IsTrue(ss.VerifyBase64SHA1Hash(hash));
            ss.AppendChar('2');
            Assert.IsFalse(ss.VerifyBase64SHA1Hash(hash));
        }
        [Test]
        public void TestUnicode() {
    
            for ( int i = 0; i < 0xFFFF; i++) {
                int expected = i;
                char c = (char)i;
                GuardedString gs = new GuardedString();
                gs = (GuardedString)SerializerUtil.CloneObject(gs);
                gs.AppendChar(c);
                gs.Access(clearChars=> {
                        int v = (int)clearChars[0];
                        Assert.AreEqual(expected, v);
                          });
    
            }
        }
        
        [Test]
        public void TestEquals() {
            GuardedString str1 = new GuardedString();
            GuardedString str2 = new GuardedString();
            Assert.AreEqual(str1, str2);
            str2.AppendChar('2');
            Assert.AreNotEqual(str1,str2);
            str1.AppendChar('2');
            Assert.AreEqual(str1, str2);
        }
        

        /**
         * Highly insecure method! Do not do this in production
         * code. This is only for test purposes
         */
        private String DecryptToString(GuardedString str) {
            StringBuilder buf = new StringBuilder();
            str.Access(
                                            array=>
                {
                    for ( int i = 0 ; i < array.Length; i++)
                    {
                        buf.Append(array[i]);
                    }
                });
            return buf.ToString();
        }
    }
}
