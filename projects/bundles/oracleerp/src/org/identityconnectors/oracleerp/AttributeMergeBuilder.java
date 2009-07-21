package org.identityconnectors.oracleerp;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;

/**
 * Builder class to create a {@link ConnectorObject}.
 * 
 * Merge is done by joining the attribute unique values list
 */
public final class AttributeMergeBuilder {

    private static final Log log = Log.getLog(AttributeMergeBuilder.class);
    private Set<String> _attrToGet = null;
    private Map<String, List<Object>> _attrs = new HashMap<String, List<Object>>();

    /**
     * @param attrToGet  the attribute merge filter
     */
    public AttributeMergeBuilder(Collection<String> attrToGet) {
        initAttributesToGet(attrToGet);
    }


    /**
     * 
     */
    public AttributeMergeBuilder() {
        //empty
    }
    

    /**
     * Adds each object in the collection.
     * 
     * @param name
     * @param obj
     * @return the builder
     */
    public AttributeMergeBuilder addAttribute(String name, Collection<?> obj) {
        if (obj != null) {
            mergeValue(name, obj);
        }
        return this;
    }

    /**
     * Adds values to the attribute.
     * 
     * @param name
     * @param objs
     * @return the builder
     */
    public AttributeMergeBuilder addAttribute(String name, Object... objs) {
        mergeValue(name, objs == null ? null : Arrays.asList(objs));
        return this;
    }

    /**
     * @param merge
     */
    private void mergeValue(String name, Collection<?> merge) {
        if (skipAttribute(name)) {
            return;
        }
        List<Object> old = _attrs.get(name);
        if (old != null) {
            if (merge != null) {
                for (Object add : merge) {
                    if (add != null && !old.contains(add)) {
                        old.add(add);
                    }
                }
            }
        } else if (merge != null) {
            _attrs.put(name, CollectionUtil.newList(merge));
        } else {
            _attrs.put(name, null);
        }
    }
    
    /**
     * Builds a 'List<Attribute>' .
     * 
     * @return the list
     */
    public List<Attribute> build() {
        // check that there are attributes to return..
        if (_attrs.size() == 0) {
            throw new IllegalStateException("No attributes set!");
        }
        final List<Attribute> attrs = CollectionUtil.newList();
        for (Entry<String, List<Object>> entry : _attrs.entrySet()) {
            final String name = entry.getKey();
            final List<Object> value = entry.getValue();
            if (skipAttribute(name)) {
                continue;
            }            
            final Attribute attr = AttributeBuilder.build(name, value);
            attrs.add(attr);
        }
        return attrs;
    }    
    
    /**
     * 
     * @param name
     * @return
     */
    private boolean skipAttribute(String name) {
        if (_attrToGet != null &&  !_attrToGet.contains(name)) {
            log.info("Skip merge attribute {0}, it is not in attributesToGet", name);
            return true;
        }    
        return false;
    }

    /**
     * Builds a 'List<Attribute>' .
     * @param attrToGet 
     * 
     * @return the list
     */
    public List<Attribute> build(Collection<String> attrToGet) {
        initAttributesToGet(attrToGet);
        return build();
    }
    

    /**
     * @param attrToGet
     */
    private void initAttributesToGet(Collection<String> attrToGet) {
        if(attrToGet != null) {
            _attrToGet = CollectionUtil.newCaseInsensitiveSet();
            for (String toGet :  attrToGet) {
                _attrToGet.add(toGet);
            }                            
        }
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        bld.append("Atttribute map:");
        bld.append(_attrs);
        bld.append(", active filter:");
        bld.append(_attrToGet);
        return bld.toString();
    } 
    
}