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
package org.identityconnectors.test.framework.impl.api.local.operations;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.impl.api.local.operations.SearchImpl;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.Assert;
import org.junit.Test;

/**
 * Attempt to test Search.
 */
public class SearchImplTests {

    @Test
    public void testEliminateDups() {
        List<ConnectorObject> data;
        // create duplicate data..
        Set<ConnectorObject> expected = new LinkedHashSet<ConnectorObject>();
        List<List<ConnectorObject>> main = new ArrayList<List<ConnectorObject>>();
        // create first batch
        data = new ArrayList<ConnectorObject>();
        for (int i = 0; i < 5; i++) {
            data.add(createObject(i));
            expected.add(createObject(i));
        }
        main.add(data);
        // create second batch
        data = new ArrayList<ConnectorObject>();
        for (int i = 3; i < 10; i++) {
            data.add(createObject(i));
            expected.add(createObject(i));
        }
        main.add(data);
        // create third batch
        data = new ArrayList<ConnectorObject>();
        for (int i = 8; i < 12; i++) {
            data.add(createObject(i));
            expected.add(createObject(i));
        }
        main.add(data);
        List<ConnectorObject> actual = TestHelpers.searchToList(
                new DuplicateProvider(), ObjectClass.ACCOUNT, new MockFilter(
                        main), null);
        List<ConnectorObject> expecteList = CollectionUtil.newList(expected);
        Assert.assertEquals(expecteList, actual);
    }

    @Test
    public void testAttrsToGetQuery() {
        // create duplicate data..
        Set<ConnectorObject> expected = new LinkedHashSet<ConnectorObject>();
        List<List<ConnectorObject>> main = new ArrayList<List<ConnectorObject>>();
        List<ConnectorObject> data = new ArrayList<ConnectorObject>();
        for (int i = 0; i < 10; i++) {
            ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
            bld.setUid("" + i);
            bld.setName("" + i);
            bld.addAttribute("a", 1);
            bld.addAttribute("b", 2);
            expected.add(bld.build());
            bld.addAttribute("c", 3);
            data.add(bld.build());
        }
        main.add(data);
        Filter filter = new MockFilter(main);
        Connector connector = new DuplicateProvider();
        SearchImpl search = new SearchImpl(null, connector);
        OperationOptionsBuilder bld = new OperationOptionsBuilder();
        bld.setAttributesToGet(new String[] {"a", "b"});
        OperationOptions options = bld.build();
        List<ConnectorObject> actual = TestHelpers.searchToList(
                search, ObjectClass.ACCOUNT, filter, options);
        List<ConnectorObject> expecteList = CollectionUtil.newList(expected);
        Assert.assertEquals(expecteList, actual);
    }

    ConnectorObject createObject(int uid) {
        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
        bld.setUid("" + uid);
        bld.setName("" + uid);
        bld.addAttribute(AttributeBuilder.build("a", uid));
        return bld.build();
    }

    public static class DuplicateProvider implements
            SearchOp<List<ConnectorObject>>, Connector {
        /**
         * Just return something..
         */
        public static class MockFilterTranslator implements
                FilterTranslator<List<ConnectorObject>> {
            public List<List<ConnectorObject>> translate(Filter filter) {
                return ((MockFilter) filter).getObjects();
            }
        }

        public FilterTranslator<List<ConnectorObject>> createFilterTranslator(
                ObjectClass oclass, OperationOptions options) {
            return new MockFilterTranslator();
        }

        public void executeQuery(ObjectClass oclass,
                List<ConnectorObject> query, ResultsHandler handler,
                OperationOptions options) {
            for (ConnectorObject obj : query) {
                if (!handler.handle(obj)) {
                    break;
                }
            }
        }

        //
        // Do nothing methods for search impl call..
        //
        public void dispose() {
            // TODO Auto-generated method stub
            
        }

        public Configuration getConfiguration() {
            // TODO Auto-generated method stub
            return null;
        }

        public void init(Configuration cfg) {
            // TODO Auto-generated method stub
            
        }
    }

    /**
     * Use the filter to pass objects to the filter translator.
     */
    public static class MockFilter implements Filter {
        public final List<List<ConnectorObject>> _objs;

        public MockFilter(List<List<ConnectorObject>> objs) {
            _objs = objs;
        }

        public boolean accept(ConnectorObject obj) {
            return true;
        }

        public List<List<ConnectorObject>> getObjects() {
            return _objs;
        }
    }
}
