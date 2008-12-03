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
package org.identityconnectors.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;


public class MockUpdateConnector implements Connector, UpdateOp, SearchOp<String> {

    private Configuration _cfg;
    
    public void dispose() {
        // nothing to do this is a mock connector..
    }

    public void init(Configuration cfg) {
        _cfg = cfg;
    }
    public Configuration getConfiguration() {
        return _cfg;
    }

    // =======================================================================
    // Test Data
    // =======================================================================
    private static List<ConnectorObject> objects = new ArrayList<ConnectorObject>();
    static {
        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
        for (int i = 0; i < 100; i++) {
            bld.setUid(Integer.toString(i));
            bld.setName(Integer.toString(i));
            objects.add(bld.build());
        }
    };

    // =======================================================================
    // Test Methods
    // =======================================================================
    /**
     * This will do a basic replace.
     * 
     * @see UpdateOp#update(ConnectorObject)
     */
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        String val = AttributeUtil.getAsStringValue(uid);
        int idx = Integer.valueOf(val).intValue();
        // get out the object..
        ConnectorObject base = objects.get(idx);
        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
        bld.add(base);
        bld.addAttributes(attrs);
        ConnectorObject obj = bld.build();
        objects.set(idx, obj);
        return obj.getUid();
    }
    
    public FilterTranslator<String> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        //no translation - ok since this is just for tests
        return new AbstractFilterTranslator<String>(){};
    }

    /**
     * Simply return everything don't bother optimizing.
     * 
     * @see SearchOp#search(Filter)
     */
    public void executeQuery(ObjectClass oclass, String query, ResultsHandler handler, OperationOptions options) {
        Iterator<ConnectorObject> iter = objects.iterator();
        while ( iter.hasNext() ) {
            if (!handler.handle(iter.next())) {
                break;
            }
        }
    }
}
