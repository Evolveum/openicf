#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package ${package};

/**
 * Class to represent a ${connectorName} Connection
 *
 * @author ${symbol_dollar}author${symbol_dollar}
 * @version ${symbol_dollar}Revision${symbol_dollar} ${symbol_dollar}Date${symbol_dollar}
 */
public class ${connectorName}Connection {

    private ${connectorName}Configuration configuration;

    public ${connectorName}Connection(${connectorName}Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Release internal resources
     */
    public void dispose() {
        //implementation
    }

    /**
     * If internal connection is not usable, throw IllegalStateException
     */
    public void test() {
        //implementation
    }

}
