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
 * Portions Copyrighted 2014 ForgeRock AS.
 */
package org.identityconnectors.framework.common.objects;

import java.util.LinkedHashMap;
import java.util.Map;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.serializer.ObjectSerializerFactory;
import org.identityconnectors.framework.common.serializer.SerializerUtil;

/**
 * Encapsulates a script and all of its parameters.
 *
 * @see org.identityconnectors.framework.api.operations.ScriptOnResourceApiOp
 * @see org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp
 */
public final class ScriptContext {

    private final String scriptLanguage;
    private final String scriptText;
    private final Map<String, Object> scriptArguments;

    /**
     * Public only for serialization; please use {@link ScriptContextBuilder}.
     *
     * @param scriptLanguage
     *            The script language. Must not be null.
     * @param scriptText
     *            The script text. Must not be null.
     * @param scriptArguments
     *            The script arguments. May be null.
     */
    public ScriptContext(String scriptLanguage, String scriptText,
            Map<String, Object> scriptArguments) {

        if (StringUtil.isBlank(scriptLanguage)) {
            throw new IllegalArgumentException("Argument 'scriptLanguage' must be specified");
        }
        if (StringUtil.isBlank(scriptText)) {
            throw new IllegalArgumentException("Argument 'scriptText' must be specified");
        }
        // clone script arguments and options - this serves two purposes
        // 1)makes sure everthing is serializable
        // 2)does a deep copy
        @SuppressWarnings("unchecked")
        Map<String, Object> scriptArgumentsClone =
                (Map<String, Object>) SerializerUtil.cloneObject(scriptArguments);
        this.scriptLanguage = scriptLanguage;
        this.scriptText = scriptText;
        this.scriptArguments = CollectionUtil.asReadOnlyMap(scriptArgumentsClone);
    }

    /**
     * Identifies the language in which the script is written (e.g.,
     * <code>bash</code>, <code>csh</code>, <code>Perl4</code> or
     * <code>Python</code>).
     *
     * @return The script language.
     */
    public String getScriptLanguage() {
        return scriptLanguage;
    }

    /**
     * Returns the text (i.e., actual characters) of the script.
     *
     * @return The text of the script.
     */
    public String getScriptText() {
        return scriptText;
    }

    /**
     * Returns a map of arguments to be passed to the script.
     *
     * Values must be types that the framework can serialize. See
     * {@link ObjectSerializerFactory} for a list of supported types.
     *
     * @return A map of arguments to be passed to the script.
     */
    public Map<String, Object> getScriptArguments() {
        return scriptArguments;
    }

    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        bld.append("ScriptContext: ");
        // poor mans to string method..
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("Language", getScriptLanguage());
        map.put("Text", getScriptText());
        map.put("Arguments", getScriptArguments());
        bld.append(map.toString());
        return bld.toString();
    }
}
