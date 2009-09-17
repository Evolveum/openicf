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
package org.identityconnectors.solaris.operation.search.nodes;

import java.util.Set;

import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;



/** 
 * Node of search filter tree for Solaris.
 * 
 * Internal representation of the tree is a Binary tree with two type of nodes:
 * <ul>
 * <li>Leaves (extending {@see AttributeNode}) of the tree contain {@link NativeAttribute}-s.</li>
 * <li>Nodes which are not leaves extend {@see BinaryOpNode}.</li>
 * </ul>
 * 
 * @author David Adam
 */
public interface Node {
    /** @return true if the attributes of 'entry' satisfy the filter's conditions. */
    public boolean evaluate(SolarisEntry entry);
    
    /** add the attributes of the node to the given set */
    public void collectAttributeNames(Set<NativeAttribute> attrs);
}