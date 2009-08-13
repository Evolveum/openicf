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
package org.identityconnectors.patternparser;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The MapTransform parses a String into a Map, using an array of PatternNodes, which contain
 * regular expressions to break up the input. The values stored in the Map can be modified by
 * an array of Transforms attached to the PatternNode.
 * <p>
 * The XML for a MapTransform looks like
 * <pre>
 * &lt;MapTransform&gt;
 *   &lt;PatternNode key='<b>key</b>' pattern='<b>pattern</b>' optional='<b>optional</b>' reset='<b>reset</b>'&gt;
 *     ...optional Transforms...
 *   &lt;/PatternNode&gt;
 *   ...optional additional PatternNodes...
 * &lt;/MapTransform&gt;
 * </pre>
 * where
 * <ul>
 * <li><b>key</b> -- The key used to store the result of the embedded Transforms in the Map. If there are no Transforms, the entire matched String is stored.</li>
 * <li><b>pattern</b> -- A pattern, which, when matched, is used as input to the first Transform stored in the PatternNode.</li>
 * <li><b>optional</b> -- A boolean indicating if matching the pattern is optional.</li>
 * <li><b>reset</b> -- If true, the the next PatternNode is matched starting at the first character after the substring matched by this PatternNode. If false, the next PatternNode is matched against the same point in the input String as this PatternNode.</li>
 * </ul>
 */
public class MapTransform extends Transform {
    
    private boolean _debug                    = false;
    
    /**
     * Store the information about a map element
     */
    public static class PatternNode {
        public static final String PATTERN_NODE   = "PatternNode";
        private static final String KEY           = "key";
        private static final String PATTERN       = "pattern";
        private static final String OPTIONAL      = "optional";
        private static final String RESET         = "reset";
        private String _key;
        private Pattern _pattern;
        private Transform[] _transforms;
        private boolean _optional;
        private boolean _reset;
        
        private String getChildren(int indent) {
            StringBuffer buffer = new StringBuffer();
            for (Transform transform : _transforms)
                buffer.append(transform.toXml(indent+2));
            return buffer.toString();
        }

        private String getAttributes() {
            return attributeToString(KEY, _key)+
                attributeToString(PATTERN, _pattern.pattern())+
                attributeToString(OPTIONAL, Boolean.toString(_optional))+
                attributeToString(RESET, Boolean.toString(_reset));
        }
        
        public String toXml(int indent) {
            String children = getChildren(indent);
            StringBuffer pad = new StringBuffer();
            for (int i=0; i<indent;i++)
                pad.append(" ");
            if (children.length()==0)
                return pad+"<"+PATTERN_NODE+getAttributes()+"/>\n";
            else 
                return pad+"<"+PATTERN_NODE+getAttributes()+">\n"+children+pad+"</"+PATTERN_NODE+">\n";
        }

        
        public PatternNode(Element element) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException  {
            this(element.getAttribute(KEY), element.getAttribute(PATTERN), Boolean.parseBoolean(element.getAttribute(OPTIONAL)), Boolean.parseBoolean(element.getAttribute(RESET)), null);
            NodeList nodeList = element.getChildNodes();
            int nodeCount = nodeList.getLength();
            List<Transform> transforms = new ArrayList<Transform>();
            for (int i=0; i<nodeCount; i++)
                if (nodeList.item(i) instanceof Element)
                    transforms.add(Transform.newTransform((Element)nodeList.item(i)));
            _transforms = transforms.toArray(new Transform[0]);
        }
        
        /**
         * Recognize a Pattern in the input, and produce a value
         * 
         * @param key -- key to store the result in a Map
         * @param pattern -- pattern for which to search
         */
        public PatternNode(String key, String pattern) {
            this(key, pattern, false, false, new Transform[0]);
        }

        /**
         * Recognize a Pattern in the input, and produce a value, after applying
         * transformations
         * 
         * @param key -- key to store the result in a Map
         * @param pattern -- pattern for which to search
         * @param optional -- indicates if the pattern is optional
         * @param reset -- indicates if the current location is not to be advanced even if the pattern patches
         * @param transforms -- an array of transformations to be applied to the result
         */
        public PatternNode(String key, String pattern, boolean optional, boolean reset, Transform[] transforms) {
            _key = key;
            _pattern = Pattern.compile(pattern);
            _optional = optional;
            _reset = reset;
            _transforms = transforms==null?new Transform[0]:transforms;            
        }
    }

    private List<PatternNode> _parseInfo;
    
    public MapTransform(Element element) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException  {
        _parseInfo = new ArrayList<PatternNode>();
        NodeList nodes = element.getChildNodes();
        for (int i=0; i<nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element) {
                _parseInfo.add(new PatternNode((Element)nodes.item(i)));
            }
        }
    }
    
    public MapTransform(List<PatternNode> parseInfo) {
        _parseInfo = parseInfo;
    }
    
    protected String getChildren(int indent) {
        StringBuffer children = new StringBuffer();
        for (PatternNode patternNode : _parseInfo) {
            children.append(patternNode.toXml(indent+2));
        }
        return children.toString();
    }
    
    private String resultToString(Object result) {
        if (result == null) {
            return "<null>";
        } else if (result.getClass().isArray()) {
            return Arrays.deepToString((Object[])result);
        } else {
            return result.toString();
        }
        
    }

    @SuppressWarnings("unchecked")
    private int parse(String inputString, Map<String, Object> output, int offset) throws Exception {
        int newOffset = offset;
        for (PatternNode patternNode : _parseInfo) {
            Matcher matcher = patternNode._pattern.matcher(inputString);
            if (matcher.find(newOffset)) {
                if (_debug) {
                    System.out.println("Matched regex '"+patternNode._pattern.pattern()+"' to '"+matcher.group()+"' at character "+matcher.start());
                    System.out.println("    Match value:'"+matcher.group(1)+"'");;
                }
                if (!patternNode._reset)
                    newOffset = matcher.end();
                Object result = matcher.group(1);
                for (Transform transform : patternNode._transforms) {
                    String oldResult = resultToString(result);
                    result = transform.transform(result);
                    if (_debug) {
                        System.out.println("    Transform "+transform.getClass().getName()+":'"+oldResult+"'->'"+resultToString(result)+"'");
                    }
                }
                
                if (patternNode._key.length()>0) {
                    output.put(patternNode._key, result);
                } else {
                    if (result instanceof Map) {
                        output.putAll((Map<String, Object>)result);
                    } else if (result instanceof List) {
                        List<?> list = (List<?>)result;
                        for (int i=0; i<list.size(); i+=2) {
                            output.put((String)list.get(i), list.get(i+1));
                        }
                    } else {
                        throw new Exception("Nameless pattern returns "+result.getClass());
                    }
                }
            } else {
                if (!patternNode._optional)
                    throw new Exception("No Match with pattern "+patternNode._pattern.toString()+" at "+newOffset);
            }
        }
        return newOffset;
    }
    
    void setDebug(boolean debug) {
        _debug = debug;
    }

    /**
     * Parse the inputString, according to the pattern information in parseInfo, and produce
     * a Map of values
     * <p>
     * First, the input is converted to a string, then, the PatternNodes are evaluated in sequence.
     *  
     * @param input -- input to be parsed
     * @return Map<String, Object> representing the parsed inputString
     * @throws Exception
     */
    public Object transform(Object input) throws Exception {
        Map<String, Object> output = new HashMap<String, Object>();
        parse(input.toString(), output, 0);
        return output;
    }
}


