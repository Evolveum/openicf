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
package org.identityconnectors.solaris.command;

import junit.framework.Assert;

import org.identityconnectors.solaris.command.PreparedCommand.Builder;
import org.junit.Test;

public class PreparedCommandTest {
    @Test
    public void testSimple() {
        Builder builder = new PreparedCommand.Builder();
        builder.add("someCommand", "otherArgument");
        Assert.assertEquals("someCommand otherArgument", builder.build().getCommand().trim());
    }
    
    @Test
    public void testFillIn() {
        Builder builder = new PreparedCommand.Builder();
        builder.add("someCommand").addFillIn("fillIn1", "fillIn2");
        PreparedCommand product = builder.build();
        try {
            product.getCommand();
        } catch (Exception ex) {
            //ok
        }
        try {
            product.getCommand("onestufftofillin");
        }  catch (Exception ex) {
            //ok
        }
        Assert.assertEquals("someCommand one two", product.getCommand("one", "two").trim());
    }
}
