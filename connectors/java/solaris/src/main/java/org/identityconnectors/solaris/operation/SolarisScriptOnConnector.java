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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.solaris.operation;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.oro.text.regex.Perl5Compiler;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;

public class SolarisScriptOnConnector {

    private final static Pattern ANY_WORD_CHARACTER_PATTERN = Pattern.compile("(\\w)+");
    private final static Pattern LINE_BREAK = Pattern.compile("\\r?\\n|\\r");

    /*
     * Environment variable names used by the utilities in the Shell and
     * Utilities volume of IEEE Std 1003.1-2001 consist solely of uppercase
     * letters, digits, and the '_' (underscore) from the characters defined in
     * Portable Character Set and do not begin with a digit. Other characters
     * may be permitted by an implementation; applications shall tolerate the
     * presence of such names.
     */
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern
            .compile("([a-zA-Z_][a-zA-Z0-9_]*)");

    private SolarisConnection connection;

    private static final char[] QUOTING_TRIGGER_CHARS = { ' ', '$', ';', '&', '|', '<', '>', '*',
        '?', '(', ')', '[', ']', '{', '}', '`' };

    private static final char[] ESCAPE_CHARS = { '\'' };

    /**
     * Supported Shell names.
     */
    enum Shell {
        sh {
            @Override
            public StringBuilder getBuilder() {
                return new StringBuilder("/bin/sh <<'ENDSSH'\n");
            }
        },
        csh {
            @Override
            public StringBuilder getBuilder() {
                return new StringBuilder("/bin/csh <<'ENDSSH'\n");
            }
        },
        ksh {
            @Override
            public StringBuilder getBuilder() {
                return new StringBuilder("/bin/ksh <<'ENDSSH'\n");
            }
        },
        bash {
            @Override
            public StringBuilder getBuilder() {
                return new StringBuilder("/bin/bash -l <<'ENDSSH'\n");
            }
        },
        tcsh {
            @Override
            public StringBuilder getBuilder() {
                return new StringBuilder("/bin/tcsh <<'ENDSSH'\n");
            }
        },
        zsh {
            @Override
            public StringBuilder getBuilder() {
                return new StringBuilder("/bin/zsh <<'ENDSSH'\n");
            }
        };

        public abstract StringBuilder getBuilder();
    }

    public SolarisScriptOnConnector(SolarisConnector solarisConnector) {
        connection = solarisConnector.getConnection();
    }

    /**
     * Execute a script on the resource. The result will contain everything up
     * to the first rootShellPrompt.
     *
     * @param request
     *            contains scriptText, that will be executed.
     * @param options
     *            is not used by {@link SolarisConnector}
     * @return the result of the script's execution (the result is the feedback
     *         up to the rootShellPrompt).
     */
    @SuppressWarnings("fallthrough")
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {
        final String scriptLanguage = request.getScriptLanguage();
        final String scriptText = request.getScriptText();

        if (StringUtil.isBlank(scriptLanguage)) {
            throw new IllegalArgumentException("Script language is missing.");
        }

        if (StringUtil.isBlank(scriptText)) {
            throw new IllegalArgumentException("scriptText is missing");
        }

        Shell shell = null;
        for (Shell s : Shell.values()) {
            if (s.name().equalsIgnoreCase(scriptLanguage)) {
                shell = s;
                break;
            }
        }

        if (null == shell) {
            throw new IllegalArgumentException("ScriptLanguage is '" + scriptLanguage
                    + "'. The only accepted script languages are: sh, csh, ksh, bash, tcsh, zsh.");
        }

        StringBuilder sb = shell.getBuilder();
        for (Map.Entry<String, Object> entry : request.getScriptArguments().entrySet()) {
            if (VARIABLE_NAME_PATTERN.matcher(entry.getKey()).matches()) {
                String value =
                        quoteForDCLWhenNeeded(null != entry.getKey() ? entry.getValue().toString()
                                : "");
                switch (shell) {
                case bash:
                case zsh:
                case ksh:
                case sh: {
                    sb.append(entry.getKey()).append("=").append(value).append('\n');
                    sb.append("export ").append(entry.getKey()).append('\n');
                    break;
                }
                case tcsh:
                case csh: {
                    sb.append("setenv ").append(entry.getKey()).append(" ").append(value).append(
                            '\n');
                    break;
                }
                }
            } else {
                throw new IllegalArgumentException("Invalid argument name: " + entry.getKey());
            }
        }

        sb.append(request.getScriptText());

        String[] lines = LINE_BREAK.split(sb.toString());
        for (String line : lines) {
            connection.executeCommand(line, CollectionUtil.newSet(Perl5Compiler
                    .quotemeta(connection.getRootShellPrompt())), CollectionUtil.newSet(">"));
        }

        return connection.executeCommand("ENDSSH");
    }

    /**
     * Solaris argument quoting.
     *
     * @param unquoted
     * @return
     */
    public static String quoteForDCLWhenNeeded(String unquoted) {
        boolean quote = !ANY_WORD_CHARACTER_PATTERN.matcher(unquoted).matches();
        if (unquoted.length() == 0) {
            quote = true;
        }

        if (!quote) {
            return unquoted;
        }
        return quoteAndEscape(unquoted, '\'', ESCAPE_CHARS, QUOTING_TRIGGER_CHARS, "'\\%s'", true);

    }

    /**
     * @param source
     * @param quoteChar
     * @param escapedChars
     * @param quotingTriggers
     * @param escapePattern
     * @param force
     * @return the String quoted and escaped
     */
    public static String quoteAndEscape(String source, char quoteChar, final char[] escapedChars,
            final char[] quotingTriggers, String escapePattern, boolean force) {
        if (source == null) {
            return null;
        }

        if (!force && source.startsWith(Character.toString(quoteChar))
                && source.endsWith(Character.toString(quoteChar))) {
            return source;
        }

        String escaped = escape(source, escapedChars, escapePattern);

        boolean quote = false;
        if (force) {
            quote = true;
        } else if (!escaped.equals(source)) {
            quote = true;
        } else {
            for (int i = 0; i < quotingTriggers.length; i++) {
                if (escaped.indexOf(quotingTriggers[i]) > -1) {
                    quote = true;
                    break;
                }
            }
        }

        if (quote) {
            return quoteChar + escaped + quoteChar;
        }

        return escaped;
    }

    /**
     * @param source
     * @param escapedChars
     * @param escapePattern
     * @return the String escaped
     */
    public static String escape(String source, final char[] escapedChars, String escapePattern) {
        if (source == null) {
            return null;
        }

        char[] eqc = new char[escapedChars.length];
        System.arraycopy(escapedChars, 0, eqc, 0, escapedChars.length);
        Arrays.sort(eqc);

        StringBuilder builder = new StringBuilder(source.length());

        for (int i = 0; i < source.length(); i++) {
            final char c = source.charAt(i);
            int result = Arrays.binarySearch(eqc, c);

            if (result > -1) {
                builder.append(String.format(escapePattern, c));
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }

}
