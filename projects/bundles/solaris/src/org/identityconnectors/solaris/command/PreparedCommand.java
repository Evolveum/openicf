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

/**
 * this class is used for building a half-filled in command, which is filled upon call.
 * @author David Adam
 */
public class PreparedCommand {
    private final String cmd;
    private final int nrOfFillIns;
    
    /** build prepared commands, that contain unfilled parts. */
    public static class Builder {
        private StringBuffer sb;
        private int nrOfFillIns;
        
        public Builder() {
            this.nrOfFillIns = 0;
            this.sb = new StringBuffer();
        }
        
        /** add a simple command or its arguments (no extra quoting applied) */
        public Builder add(String... commandToken) {
            for (String string : commandToken) {
                sb.append(string).append(" ");
            }
            return this;
        }
        
        /** add special fillInTokens */
        public Builder addFillIn(String... fillInToken) {
            for (String string : fillInToken) {
                sb.append(createSpecial(string)).append(" ");
                nrOfFillIns++;
            }
            return this;
        }
        
        private static String createSpecial(String s) {
            return String.format("__%s__", s);
        }
        
        public PreparedCommand build() {
            return new PreparedCommand(this);
        }
    }// end of Builder
    
    private PreparedCommand(Builder builder) {
        this.nrOfFillIns = builder.nrOfFillIns;
        this.cmd = builder.sb.toString();
    }
    
    /**
     * acquire the command
     * @return the command (without fill-in parts
     * @throws IllegalArgumentException in case of unfilled parts in the command
     */
    public String getCommand() throws IllegalArgumentException {
        return getCommand(new String[0]);
    }
    
    /** 
     * acquire the command, see {@link PreparedCommand#getCommand()}
     * @param fillStrings the strings that should fill the empty spaces.
     */
    public String getCommand(String... fillStrings) {
        String result = new String(cmd);
        if (fillStrings.length == nrOfFillIns) {
            for (String string : fillStrings) {
                result = result.replaceFirst("__[^_]+__", string);
            }
            return result;
        } else throw new IllegalArgumentException("Error in call of PreparedCommand. You should supply the undefined fill-in attributes.");
    }
}
