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
 * Portions Copyrighted 2011 Viliam Repan (lazyman)
 * 
 * $Id$
 */
package com.evolveum.polygon.csvfile.sync;

import com.evolveum.polygon.csvfile.util.Utils;

import java.util.List;

/**
 *
 * @author Viliam Repan (lazyman)
 */
public class Change {

    public enum Type {

        DELETE, CREATE, MODIFY;
    }
    private String uid;
    private Type type;
    private List<String> header;
    private List<String> attributes;
	private int position;				// for CREATE/MODIFY: position in 'new' file; for DELETE: position in 'old' file

    public Change(String uid, Type type, List<String> header, int position) {
        this(uid, type, header, null, position);
    }

    public Change(String uid, Type type, List<String> header, List<String> attributes, int position) {
        Utils.notNullArgument(uid, "uid");
        Utils.notNullArgument(type, "type");
        Utils.notNullArgument(header, "header");

        this.uid = uid;
        this.type = type;
        this.header = header;
        this.attributes = attributes;
		this.position = position;
    }

    public String getUid() {
        return uid;
    }

    public Type getType() {
        return type;
    }

    public List<String> getHeader() {
        return header;
    }

    public List<String> getAttributes() {
        return attributes;
    }

	public int getPosition() {
		return position;
	}
}
