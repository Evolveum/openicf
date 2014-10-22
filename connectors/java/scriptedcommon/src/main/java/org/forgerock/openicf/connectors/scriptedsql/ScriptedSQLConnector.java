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

import java.sql.Connection;

import org.forgerock.openicf.misc.scriptedcommon.ScriptedConnectorBase;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;

import groovy.lang.Binding;
import groovy.lang.Closure;

/**
 * Main implementation of the ScriptedSQL Connector.
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 */
@ConnectorClass(displayNameKey = "groovy.sql.connector.display",
        configurationClass = ScriptedSQLConfiguration.class, messageCatalogPaths = {
            "org/forgerock/openicf/connectors/groovy/Messages",
            "org/forgerock/openicf/connectors/scriptedsql/Messages" })
public class ScriptedSQLConnector extends ScriptedConnectorBase<ScriptedSQLConfiguration> implements
        Connector {

    protected Object evaluateScript(String scriptName, Binding arguments,
            Closure<Object> scriptEvaluator) throws Exception {
        final Connection c =
                ((ScriptedSQLConfiguration) getScriptedConfiguration()).getDataSource()
                        .getConnection();
        try {
            arguments.setVariable(CONNECTION, c);
            return scriptEvaluator.call(scriptName, arguments);
        } finally {
            // Put back the connection to pool
            c.close();
        }
    }
}
