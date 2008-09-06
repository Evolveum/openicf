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
package org.identityconnectors.framework.spi;

/**
 * This is the main interface to declare a connector. Developers must implement
 * this interface. The life-cycle for a {@link Connector} is as follows
 * {@link #init(Configuration)} is called then any of the operations implemented
 * in the Connector and finally dispose. The {@link #init(Configuration)} and
 * {@link #dispose()} allow for block operations. For instance bulk creates or
 * deletes and the use of before and after actions. Once {@link #dispose()} is
 * called the {@link Connector} object is discarded.
 */
public interface Connector {

    /**
     * Return the configuration that was passed to {@link #init(Configuration)}.
     * 
     * @return The configuration that was passed to {@link #init(Configuration)}.
     */
    public Configuration getConfiguration();

    /**
     * Initialize the connector with its configuration. For instance in a JDBC
     * {@link Connector} this would include the database URL, password, and
     * user.
     * 
     * @param cfg
     *            instance of the {@link Configuration} object implemented by
     *            the {@link Connector} developer and populated with information
     *            in order to initialize the {@link Connector}.
     */
    public void init(Configuration cfg);

    /**
     * Dispose of any resources the {@link Connector} uses.
     */
    public void dispose();
}
