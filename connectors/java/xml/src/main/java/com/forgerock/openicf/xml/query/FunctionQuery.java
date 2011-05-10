/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * $Id$
 */

package com.forgerock.openicf.xml.query;

import com.forgerock.openicf.xml.query.abstracts.QueryPart;

public class FunctionQuery implements QueryPart {

    private String [] args;
    private String function;
    private boolean not;
   
    
    public FunctionQuery(String [] args, String function, boolean not) {
        this.args = args;
        this.function = function;
        this.not = not;
    }

    // creates function-expression.
    // all args have to be prefixed with $x/, '', etc
    @Override
    public String getExpression() {
        if (not) {
            return createFalseExpression();
        }
        else {
            return createTrueExpression();
        }
    }

    private String createTrueExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("fn:");
        sb.append(this.function);
        sb.append("(");
        addArgs(sb);
        sb.append(")");

        return sb.toString();
    }

    private String createFalseExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("fn:");
        sb.append("not(");
        sb.append(this.function);
        sb.append("(");
        addArgs(sb);
        sb.append("))");

        return sb.toString();
    }

    private void addArgs(StringBuilder sb) {
        // add args to function
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i]);
            if (i < args.length-1)
                sb.append(", ");
        }
    }
}