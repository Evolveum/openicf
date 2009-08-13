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
package org.identityconnectors.solaris.command.pattern;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;

import expect4j.Closure;
import expect4j.matches.RegExpMatch;

/**
 * extends the Expect4J's matcher with case insensitive match.
 * @author David Adam
 */
public class RegExpCaseInsensitiveMatch extends RegExpMatch {

    public RegExpCaseInsensitiveMatch(String patternStr, Closure closure)
            throws MalformedPatternException {
        super(patternStr, closure);
    }
    
    @Override
    public Pattern compilePattern(String patternStr)
            throws MalformedPatternException {
        Perl5Compiler compiler = getCompiler();
        // adding case insensitivity mask
        return compiler.compile(patternStr, Perl5Compiler.DEFAULT_MASK|Perl5Compiler.SINGLELINE_MASK|Perl5Compiler.CASE_INSENSITIVE_MASK);
    }

}
