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

package org.identityconnectors.oracle;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.test.common.TestHelpers;

/**
 * @author kitko
 *
 */
class DataSourceMockHelper {
    private DataSourceMockHelper(){}
    
    static final String[] dsJNDIEnv = new String[]{"java.naming.factory.initial=" + MockContextFactory.class.getName()};
    
    public static class MockContextFactory implements InitialContextFactory{
        public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
            Context context = (Context)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Context.class}, new ContextIH());
            return context;
        }
    }
    
    static OracleConfiguration createDataSourceConfiguration(){
        OracleConfiguration conf = new OracleConfiguration();
        conf.setConnectorMessages(TestHelpers.createDummyMessages());
        conf.setDataSource("testDS");
        conf.setDsJNDIEnv(DataSourceMockHelper.dsJNDIEnv);
        conf.setPort(null);
        conf.setDriver(null);
        return conf;
    }

    
    private static class ContextIH implements InvocationHandler{

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(method.getName().equals("lookup")){
                return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{DataSource.class}, new DataSourceIH());
            }
            return null;
        }
    }
    
    private static class DataSourceIH implements InvocationHandler{
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(method.getName().equals("getConnection")){
                if(method.getParameterTypes().length == 0){
                    return OracleConfigurationTest.createThinConfiguration().createAdminConnection();
                }
                else if(method.getParameterTypes().length == 2){
                    String user = (String) args[0];
                    String password = (String) args[1];
                    return OracleConfigurationTest.createThinConfiguration().createConnection(user, new GuardedString(password.toCharArray()));
                }
            }
            throw new IllegalArgumentException("Invalid method");
        }
    }

}
