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
package org.identityconnectors.solaris.constants;

import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.operation.search.PatternBuilder;
import org.identityconnectors.solaris.operation.search.SearchPerformer.SearchCallback;

/**
 * abstraction of all Solaris Object type attributes (including GROUP, ACCOUNT).
 * 
 * @author David Adam
 */
public interface SolarisAttribute {
    /** @return the name of the GROUP attribute */
    public abstract String getName();

    /**
     * get command for acquiring the {@link Uid}-s and the given attribute from
     * Solaris OS.
     * 
     * @param fillInAttributes
     *            optional attributes that are filled in the command. (it
     *            depends on a certain attribute's unix get command, if they'll
     *            be used). Placeholders for attributes to fill in are marked
     *            with "__AttributeName__" marker in the command.
     * @return a command that just needs a few insertions and then can be
     *         executed.
     */
    public abstract String getCommand(String... fillInAttributes);

    /**
     * @return regular expression that extracts from the output of command
     *         {@link SolarisAttribute#getCommand(String...)} the required Uid
     *         and attribute in this order.
     *         
     * Note: This regular expression is built by {@link PatternBuilder}.
     */
    public abstract String getRegExpForUidAndAttribute();
    
    /**
     * @return the callback method {@see SearchCallback} this method is uded to 
     * parse the output lines of unix commands.
     */
    public abstract SearchCallback getCallbackMethod();
}