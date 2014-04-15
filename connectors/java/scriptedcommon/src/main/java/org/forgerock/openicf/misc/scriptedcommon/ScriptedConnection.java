/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All rights reserved.
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

package org.forgerock.openicf.misc.scriptedcommon;

/**
 * @author Gael Allioux <gael.allioux@forgerock.com>
 */
public interface ScriptedConnection<T> {

    /**
     * Release internal resources.
     */
    public void dispose();

    /**
     * If internal connection is not usable, throw IllegalStateException.
     */
    public void test();

    /**
     * The connection needs to provide a generic handler object that will be used by every scripts to connect to the
     * remote system
     *
     * @return the connection handler generic Object
     */
    public T getConnectionHandler();

}
