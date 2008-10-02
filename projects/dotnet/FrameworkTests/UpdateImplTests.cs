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
using NUnit.Framework;
using NUnit.Framework.SyntaxHelpers;
using System.Collections.Generic;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Api;
using Org.IdentityConnectors.Framework.Api.Operations;
using Org.IdentityConnectors.Framework.Impl.Api.Local.Operations;
using Org.IdentityConnectors.Framework.Common.Objects;
namespace FrameworkTests
{
    /// <summary>
    /// Description of UpdateImplTests.
    /// </summary>
    [TestFixture]
    public class UpdateImplTests
    {    
        [Test]
        [ExpectedException(typeof(ArgumentNullException ))]
        public void ValidateObjectClassArg() {
            UpdateImpl.ValidateInput(UpdateApiType.ADD, null, new HashSet<ConnectorAttribute>());
        }
        
        [Test]
        [ExpectedException(typeof(ArgumentNullException ))]
        public void ValidateAttrsArg() {
            UpdateImpl.ValidateInput(UpdateApiType.ADD, ObjectClass.ACCOUNT, null);
        }
        
        [Test]
        [ExpectedException(typeof(ArgumentException ))]
        public void ValidateNoUidAttribute() {
            UpdateImpl.ValidateInput(UpdateApiType.ADD, ObjectClass.ACCOUNT, new HashSet<ConnectorAttribute>());
        }
        
        [Test]
        [ExpectedException(typeof(ArgumentException ))]
        public void ValidateAddWithNullAttribute() {
            ICollection<ConnectorAttribute> attrs = new HashSet<ConnectorAttribute>();
            attrs.Add(ConnectorAttributeBuilder.Build("something"));
            UpdateImpl.ValidateInput(UpdateApiType.ADD, ObjectClass.ACCOUNT, attrs);        
        }
    
        [Test]
        [ExpectedException(typeof(ArgumentException ))]
        public void ValidateDeleteWithNullAttribute() {
            ICollection<ConnectorAttribute> attrs = new HashSet<ConnectorAttribute>();
            attrs.Add(ConnectorAttributeBuilder.Build("something"));
            UpdateImpl.ValidateInput(UpdateApiType.DELETE, ObjectClass.ACCOUNT, attrs);        
        }
    
        [Test]
        [ExpectedException(typeof(ArgumentException))]
        public void ValidateAttemptToAddName() {
            ICollection<ConnectorAttribute> attrs = new HashSet<ConnectorAttribute>();
            attrs.Add(new Name("fadf"));
            attrs.Add(new Uid(1 + ""));
            UpdateImpl.ValidateInput(UpdateApiType.ADD, ObjectClass.ACCOUNT, attrs);                
        }
    
        [Test]
        [ExpectedException(typeof(ArgumentException))]
        public void ValidateAttemptToDeleteName() {
            ICollection<ConnectorAttribute> attrs = new HashSet<ConnectorAttribute>();
            attrs.Add(new Name("fadf"));
            attrs.Add(new Uid(1 + ""));
            UpdateImpl.ValidateInput(UpdateApiType.DELETE, ObjectClass.ACCOUNT, attrs);                
        }

        [Test]
        public void ValidateAttemptToAddDeleteOperationalAttribute() {
            // list of all the operational attributes..
            ICollection<ConnectorAttribute> list = new List<ConnectorAttribute>();
            list.Add(ConnectorAttributeBuilder.BuildEnabled(false));
            list.Add(ConnectorAttributeBuilder.BuildLockOut(true));
            list.Add(ConnectorAttributeBuilder.BuildCurrentPassword(newSecureString("fadsf")));
            list.Add(ConnectorAttributeBuilder.BuildPasswordExpirationDate(DateTime.Now));
            list.Add(ConnectorAttributeBuilder.BuildPassword(newSecureString("fadsf")));
            foreach (ConnectorAttribute attr in list) {
                ICollection<ConnectorAttribute> attrs = new HashSet<ConnectorAttribute>();
                attrs.Add(attr);
                attrs.Add(new Uid(1 + ""));
                try {
                    UpdateImpl.ValidateInput(UpdateApiType.DELETE, ObjectClass.ACCOUNT, attrs);
                    Assert.Fail("Failed: " + attr.Name);
                } catch (ArgumentException) {
                    // this is a good thing..
                }
            }
        }
        
        private static SecureString newSecureString(string password) {
            SecureString rv = new SecureString();
            foreach (char c in password.ToCharArray()) {
                rv.AppendChar(c);
            }
            return rv;
        }

        [Test]
        [ExpectedException(typeof(ArgumentException))]
        public void ValidateAttemptToAddNull() {
            ICollection<ConnectorAttribute> attrs = new HashSet<ConnectorAttribute>();
            attrs.Add(ConnectorAttributeBuilder.Build("something w/ null"));
            attrs.Add(new Uid(1 + ""));
            UpdateImpl.ValidateInput(UpdateApiType.ADD, ObjectClass.ACCOUNT, attrs);        
        }
        
        [Test]
        [ExpectedException(typeof(ArgumentException))]
        public void ValidateAttemptToDeleteNull() {
            ICollection<ConnectorAttribute> attrs = new HashSet<ConnectorAttribute>();
            attrs.Add(ConnectorAttributeBuilder.Build("something w/ null"));
            attrs.Add(new Uid(1 + ""));
            UpdateImpl.ValidateInput(UpdateApiType.DELETE, ObjectClass.ACCOUNT, attrs);        
        }
    
        /// <summary>
        /// Validate two collections are equal.  (Not fast but effective)
        /// </summary>
        public static bool AreEqual(ICollection<ConnectorAttribute> arg1, 
                                    ICollection<ConnectorAttribute> arg2) {
            if (arg1.Count != arg2.Count) {
                return false;
            }
            foreach (ConnectorAttribute attr in arg1) {
                if (!arg2.Contains(attr)) {
                    return false;
                }
            }
            return true;
        }
        [Test]
        public void MergeAddAttribute() {
            UpdateImpl up = new UpdateImpl(null, null);
            ICollection<ConnectorAttribute> actual;
            ConnectorAttribute uid = new Uid(1 + "");
            ICollection<ConnectorAttribute> baseAttrs = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> expected = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> changeset = CollectionUtil.NewSet(uid);
            // attempt to add a value to an attribute..
            ConnectorAttribute cattr = ConnectorAttributeBuilder.Build("abc", 2);
            changeset.Add(cattr);
            expected.Add(ConnectorAttributeBuilder.Build("abc", 2));        
            actual = up.Merge(UpdateApiType.ADD, changeset, baseAttrs);
            Assert.IsTrue(AreEqual(expected, actual));
        }
    
        [Test]
        public void MergeAddToExistingAttribute() {
            UpdateImpl up = new UpdateImpl(null, null);
            ICollection<ConnectorAttribute> actual;
            ConnectorAttribute uid = new Uid(1 + "");
            ICollection<ConnectorAttribute> baseAttrs = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> expected = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> changeset = CollectionUtil.NewSet(uid);
            // attempt to add a value to an attribute..
            ConnectorAttribute battr = ConnectorAttributeBuilder.Build("abc", 1);
            ConnectorAttribute cattr = ConnectorAttributeBuilder.Build("abc", 2);
            baseAttrs.Add(battr);
            changeset.Add(cattr);
            expected.Add(ConnectorAttributeBuilder.Build("abc", 1, 2));        
            actual = up.Merge(UpdateApiType.ADD, changeset, baseAttrs);
            Assert.IsTrue(AreEqual(expected, actual));
        }
        
        [Test]
        public void MergeDeleteNonExistentAttribute() {
            UpdateImpl up = new UpdateImpl(null, null);
            ICollection<ConnectorAttribute> actual;
            ConnectorAttribute uid = new Uid(1 + "");
            ICollection<ConnectorAttribute> baseAttrs = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> expected = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> changeset = CollectionUtil.NewSet(uid);
            // attempt to add a value to an attribute..
            ConnectorAttribute cattr = ConnectorAttributeBuilder.Build("abc", 2);
            changeset.Add(cattr);
            actual = up.Merge(UpdateApiType.DELETE, changeset, baseAttrs);
            Assert.IsTrue(AreEqual(expected, actual));
        }
    
        [Test]
        public void MergeDeleteToExistingAttribute() {
            UpdateImpl up = new UpdateImpl(null, null);
            ICollection<ConnectorAttribute> actual;
            ConnectorAttribute uid = new Uid(1 + "");
            ICollection<ConnectorAttribute> baseAttrs = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> expected = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> changeset = CollectionUtil.NewSet(uid);
            // attempt to add a value to an attribute..
            ConnectorAttribute battr = ConnectorAttributeBuilder.Build("abc", 1, 2);
            ConnectorAttribute cattr = ConnectorAttributeBuilder.Build("abc", 2);
            baseAttrs.Add(battr);
            changeset.Add(cattr);
            expected.Add(ConnectorAttributeBuilder.Build("abc", 1));
            actual = up.Merge(UpdateApiType.DELETE, changeset, baseAttrs);
            Assert.IsTrue(AreEqual(expected, actual));
        }
    
        [Test]
        public void MergeDeleteToExistingAttributeCompletely() {
            UpdateImpl up = new UpdateImpl(null, null);
            ICollection<ConnectorAttribute> actual;
            ConnectorAttribute uid = new Uid(1 + "");
            ICollection<ConnectorAttribute> baseAttrs = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> expected = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> changeset = CollectionUtil.NewSet(uid);
            // attempt to add a value to an attribute..
            ConnectorAttribute battr = ConnectorAttributeBuilder.Build("abc", 1, 2);
            ConnectorAttribute cattr = ConnectorAttributeBuilder.Build("abc", 1, 2);
            baseAttrs.Add(battr);
            changeset.Add(cattr);
            expected.Add(ConnectorAttributeBuilder.Build("abc"));
            actual = up.Merge(UpdateApiType.DELETE, changeset, baseAttrs);
            Assert.IsTrue(AreEqual(expected, actual));
        }
    
        [Test]
        public void MergeReplaceExistingAttribute() {
            UpdateImpl up = new UpdateImpl(null, null);
            ICollection<ConnectorAttribute> actual;
            ConnectorAttribute uid = new Uid(1 + "");
            ICollection<ConnectorAttribute> baseAttrs = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> expected = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> changeset = CollectionUtil.NewSet(uid);
            // attempt to add a value to an attribute..
            ConnectorAttribute battr = ConnectorAttributeBuilder.Build("abc", 1, 2);
            ConnectorAttribute cattr = ConnectorAttributeBuilder.Build("abc", 2);
            baseAttrs.Add(battr);
            changeset.Add(cattr);
            expected.Add(ConnectorAttributeBuilder.Build("abc", 2));
            actual = up.Merge(UpdateApiType.REPLACE, changeset, baseAttrs);
            Assert.IsTrue(AreEqual(expected, actual));
        }
        
        [Test]
        public void MergeReplaceNonExistentAttribute() {
            UpdateImpl up = new UpdateImpl(null, null);
            ICollection<ConnectorAttribute> actual;
            ConnectorAttribute uid = new Uid(1 + "");
            ICollection<ConnectorAttribute> baseAttrs = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> expected = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> changeset = CollectionUtil.NewSet(uid);
            // attempt to add a value to an attribute..
            ConnectorAttribute cattr = ConnectorAttributeBuilder.Build("abc", 2);
            changeset.Add(cattr);
            expected.Add(ConnectorAttributeBuilder.Build("abc", 2));
            actual = up.Merge(UpdateApiType.REPLACE, changeset, baseAttrs);
            Assert.IsTrue(AreEqual(expected, actual));
        }
        
        [Test]
        public void MergeReplaceAttributeRemoval() {
            UpdateImpl up = new UpdateImpl(null, null);
            ICollection<ConnectorAttribute> actual;
            ConnectorAttribute uid = new Uid(1 + "");
            ICollection<ConnectorAttribute> baseAttrs = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> expected = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> changeset = CollectionUtil.NewSet(uid);
            // attempt to add a value to an attribute..
            ConnectorAttribute battr = ConnectorAttributeBuilder.Build("abc", 1, 2);
            ConnectorAttribute cattr = ConnectorAttributeBuilder.Build("abc");
            baseAttrs.Add(battr);
            changeset.Add(cattr);
            expected.Add(ConnectorAttributeBuilder.Build("abc"));
            actual = up.Merge(UpdateApiType.REPLACE, changeset, baseAttrs);
            Assert.IsTrue(AreEqual(expected, actual));
        }
        
        [Test]
        public void MergeReplaceSameAttribute() {
            UpdateImpl up = new UpdateImpl(null, null);
            ICollection<ConnectorAttribute> actual;
            ConnectorAttribute uid = new Uid(1 + "");
            ICollection<ConnectorAttribute> baseAttrs = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> expected = CollectionUtil.NewSet(uid);
            ICollection<ConnectorAttribute> changeset = CollectionUtil.NewSet(uid);
            // attempt to add a value to an attribute..
            ConnectorAttribute battr = ConnectorAttributeBuilder.Build("abc", 1);
            ConnectorAttribute cattr = ConnectorAttributeBuilder.Build("abc", 1);
            baseAttrs.Add(battr);
            changeset.Add(cattr);
            expected.Add(cattr);
            actual = up.Merge(UpdateApiType.REPLACE, changeset, baseAttrs);
            Assert.IsTrue(AreEqual(expected, actual));
        }
    }
}
