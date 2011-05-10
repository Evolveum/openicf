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



public abstract class BinaryOpNode implements Node {

    private Node left;
    private Node right;
    
    public BinaryOpNode(Node left, Node right) {
        this.left = left;
        this.right = right;
    }
    
    public Node getLeft() {
        return left;
    }
    
    public Node getRight() {
        return right;
    }
    
    /**
     * {@see org.identityconnectors.solaris.operation.search.nodes.Node#collectAttributeNames(java.util.Set)}
     */
    public void collectAttributeNames(Set<NativeAttribute> attrs) {
        left.collectAttributeNames(attrs);
        right.collectAttributeNames(attrs);
    }
    
    @Override
    public String toString() {
        return String.format("Filter %s: [left=\"%s\", right=\"%s\"]", getClass().getName(), left.toString(), right.toString());
    }
}
