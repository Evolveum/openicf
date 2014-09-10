/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2014 ForgeRock AS. All Rights Reserved
*
* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License). You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the License at
* http://forgerock.org/license/CDDLv1.0.html
* See the License for the specific language governing
* permission and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at http://forgerock.org/license/CDDLv1.0.html
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* your own identifying information:
* " Portions Copyrighted [year] [name of copyright owner]"
*
*/
package org.forgerock.openicf.connectors.scriptedsql;

import org.forgerock.openicf.misc.scriptedcommon.ScriptedConnector;

import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;

/**
 * Main implementation of the ScriptedSQL Connector.
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 *
 */
@ConnectorClass(displayNameKey = "scripted.sql.connector.display", configurationClass = ScriptedSQLConfiguration.class)
public class ScriptedSQLConnector extends ScriptedConnector implements PoolableConnector {
    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @param configuration the new {@link Configuration}
     * @see org.identityconnectors.framework.spi.Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    @Override
    public void init(final Configuration configuration) {
        this.connection = new ScriptedSQLConnection((ScriptedSQLConfiguration)configuration);
        super.init(configuration);
    }

    @Override
    public void checkAlive() {
        connection.test();
    }

}
