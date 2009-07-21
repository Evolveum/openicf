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

import java.util.ArrayList;
import java.util.List;

import org.apache.oro.text.regex.MalformedPatternException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.command.closure.ClosureFactory;
import org.identityconnectors.solaris.command.pattern.RegExpCaseInsensitiveMatch;

import expect4j.Closure;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import expect4j.matches.TimeoutMatch;

/**
 * Use this class to construct a sequence of matchers.
 * Matchers consists of two parts:
 * <ul>
 *  <li>1) regular expression -- used to detect the match,</li>
 *  <li>2) closure -- call-back interface that is executed upon the match.</li>
 * </ul>
 * @author David Adam
 */
public final class MatchBuilder {
    private List<Match> matches;

    public MatchBuilder() {
        matches = new ArrayList<Match>();
    }

    /**
     * adds a case sensitive matcher. Compare with
     * {@link MatchBuilder#addCaseInsensitiveRegExpMatch(String, Closure)}.
     */
    public void addRegExpMatch(String regExp, Closure closure) {
        try {
            matches.add(new RegExpMatch(regExp, closure));
        } catch (MalformedPatternException ex) {
            ConnectorException.wrap(ex);
        }
    }
    
    /**
     * adds a case *insensitive matcher. Compare with
     * {@link MatchBuilder#addRegExpMatch(String, Closure)}
     */
    public void addCaseInsensitiveRegExpMatch(String regExp, Closure closure) {
        try {
            matches.add(new RegExpCaseInsensitiveMatch(regExp, closure));
        } catch (MalformedPatternException ex) {
            ConnectorException.wrap(ex);
        }
    }
    
    /** add a timeout match with given 'millis' period. */
    public void addTimeoutMatch(long millis, String message) {
        matches.add(new TimeoutMatch(millis, ClosureFactory.newConnectorException(message)));
    }
    
    /** concatenate all the given matches with existing ones. */
    public void addMatches(Match[] matches) {
        for (Match match : matches) {
            this.matches.add(match);
        }
    }

    public Match[] build() {
        return matches.toArray(new Match[matches.size()]);
    }
    
    /** convenience method for a single RegExpMatcher */
    public static Match[] buildRegExpMatch(String regExp, Closure closure) {
        MatchBuilder mb = new MatchBuilder();
        mb.addRegExpMatch(regExp, closure);
        return mb.build();
    }
    
    /** convenience method for a single case insensitive RegExpMatcher */
    public static Match[] buildCaseInsensitiveRegExpMatch(String regExp, Closure closure) {
        MatchBuilder mb = new MatchBuilder();
        mb.addCaseInsensitiveRegExpMatch(regExp, closure);
        return mb.build();
    }
}
