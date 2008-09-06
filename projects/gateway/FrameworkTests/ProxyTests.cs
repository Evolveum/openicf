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
using NUnit.Framework;
using NUnit.Framework.SyntaxHelpers;
using System.Reflection;
using System.Reflection.Emit;
using Org.IdentityConnectors.Common.Proxy;
using System.Collections.Generic;
using Org.IdentityConnectors.Common;
namespace FrameworkTests
{
    public interface MyTestInterface
    {
        string TestProperty {get;set;}
        
        String TestTwoArgs(string val1, string val2);
        void TestVoid(IList<object> list);
        int TestPrimitive(int arg);
        DateTime TestStruct(DateTime arg);
    }
    
    [TestFixture]
    public class ProxyTests
    {
        public class MyHandler : InvocationHandler
        {
            private string _testProperty;
            
            public Object Invoke(Object proxy, MethodInfo method, Object [] args)
            {
                if (method.Name.Equals("TestTwoArgs"))
                {
                    return ""+method.Name+" "+args[0]+" "+args[1];
                }
                else if ( method.Name.Equals("TestVoid") )
                {
                    IList<object> arg = (IList<object>)args[0];
                    arg.Add("my void result");
                    return null;
                }
                else if ( method.Name.Equals("get_TestProperty") )
                {
                    return _testProperty;
                }
                else if ( method.Name.Equals("set_TestProperty") ) 
                {
                    _testProperty = (string)args[0];
                    return null;
                }
                else
                {
                    return args[0];
                }
            }
        }
        
        [Test]
        public void TestProxy()
        {
            InvocationHandler handler = new MyHandler();
            
            MyTestInterface inter = 
                (MyTestInterface)Proxy.NewProxyInstance(typeof(MyTestInterface),
                                                        handler);
            Assert.AreEqual("TestTwoArgs foo bar",
                            inter.TestTwoArgs("foo","bar"));
            IList<object> temp = new List<object>();
            inter.TestVoid(temp);
            Assert.AreEqual("my void result",temp[0]);
            Assert.AreEqual(10,inter.TestPrimitive(10));
            Assert.AreEqual(1000L,inter.TestStruct(new DateTime(1000)).Ticks);
            inter.TestProperty = "my property";
            Assert.AreEqual("my property",inter.TestProperty);
        }
        
    }
    
    
}
