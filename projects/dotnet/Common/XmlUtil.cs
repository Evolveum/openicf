/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
using System;
using System.Xml;
using System.Text;
namespace Org.IdentityConnectors.Common
{
    /// <summary>
    /// Description of XmlUtil.
    /// </summary>
    public static class XmlUtil
    {
        /////////////////////////////////////////////////////////////
        //
        // DOM Navigation utilities
        //
        ////////////////////////////////////////////////////////////
    
        /**
         * Return the value of an attribute on an element. <p/> The DOM getAttribute
         * method returns an empty string if the attribute doesn't exist. Here, we
         * detect this and return null.
         */
        public static String GetAttribute(XmlElement e, String name) {
            String value = e.GetAttribute(name);
            if (value != null && value.Length == 0)
                value = null;
            return value;
        }
    
        /**
         * Find an immediate child of the given name
         */
        public static XmlElement FindImmediateChildElement(XmlNode node, String name) {
    
            XmlElement found = null;
            
            if (node != null) {
    
                for (XmlNode child = node.FirstChild; child != null
                        && found == null; child = child.NextSibling) {
    
                    if (child.NodeType == XmlNodeType.Element) {
                        XmlElement tmp = (XmlElement) child;
                        if ( tmp.LocalName.Equals(name) ) {
                            return tmp;
                        }
                    }
                }
            }
    
            return found;
        }
    
        /**
         * Returns the First child element or null if none found
         * @param node The node. May be null.
         * @return the First child element or null if none found
         */
        public static XmlElement GetFirstChildElement(XmlNode node) {
            if ( node == null ) {
                return null;
            }
            XmlNode child = node.FirstChild;
            if ( child != null && child.NodeType == XmlNodeType.Element ) {
                return (XmlElement)child;
            }
            else {
                return GetNextElement(child);
            }
        }
        
        /**
         * Get the next right sibling that is an element.
         */
        public static XmlElement GetNextElement(XmlNode node) {
    
            XmlElement found = null;
    
            if (node != null) {
    
                for (XmlNode next = node.NextSibling; next != null
                        && found == null; next = next.NextSibling) {
    
                    if (next.NodeType == XmlNodeType.Element)
                        found = (XmlElement) next;
                }
            }
    
            return found;
        }
    
        /**
         * Locate the first text node at any level below the given node. If the
         * ignoreEmpty flag is true, we will ignore text nodes that contain only
         * whitespace characteres. <p/> Note that if you're trying to extract
         * element content, you probably don't want this since parser's can break up
         * pcdata into multiple adjacent text nodes. See getContent() for a more
         * useful method.
         */
        private static XmlText FindText(XmlNode node, bool ignoreEmpty) {
    
            XmlText found = null;
    
            if (node != null) {
    
                if (node.NodeType == XmlNodeType.Text
                        || node.NodeType == XmlNodeType.CDATA) {
    
                    XmlText t = (XmlText) node;
                    if (!ignoreEmpty)
                        found = t;
                    else {
                        String s = t.Data.Trim();
                        if (s.Length > 0)
                            found = t;
                    }
                }
    
                if (found == null) {
    
                    for (XmlNode child = node.FirstChild; child != null
                            && found == null; child = child.NextSibling) {
    
                        found = FindText(child, ignoreEmpty);
                    }
                }
            }
    
            return found;
        }
    
    
        /**
         * Return the content of the given element. <p/> We will descend to an
         * arbitrary depth looking for the first text node. <p/> Note that
         * the parser may break what was originally a single string of pcdata into
         * multiple adjacent text nodes. Xerces appears to do this when it
         * encounters a '$' in the text, not sure if there is specified behavior, or
         * if its parser specific. <p/> Here, we will congeal adjacent text nodes.
         * <p/> We will NOT ignore text nodes that have only whitespace.
         */
        public static String GetContent(XmlElement e) {
    
            String content = null;
    
            if (e != null) {
    
                // find the first inner text node,
                XmlText t = FindText(e, false);
                if (t != null) {
                    // we have at least some text
                    StringBuilder b = new StringBuilder();
                    while (t != null) {
                        b.Append(t.Data);
                        XmlNode n = t.NextSibling;
    
                        t = null;
                        if (n != null
                                && ((n.NodeType == XmlNodeType.Text) || 
                                        (n.NodeType == XmlNodeType.CDATA))) {
                            t = (XmlText) n;
                        }
                    }
                    content = b.ToString();
                }
            }
    
            return content;
        }
    }
}
