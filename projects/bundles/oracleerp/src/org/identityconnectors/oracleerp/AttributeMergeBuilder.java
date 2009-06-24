package org.identityconnectors.oracleerp;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Map.Entry;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;

/**
 * Builder class to create a {@link ConnectorObject}.
 * 
 * Merge is done by joining the attribute unique values list
 */
public final class AttributeMergeBuilder {

    private SortedSet<String> _attrToGet = CollectionUtil.newCaseInsensitiveSet();
    private Map<String, List<Object>> _attrs;

    /**
     * @param attrToGet  the attribute merge filter
     */
    public AttributeMergeBuilder(Collection<String> attrToGet) {
        if(attrToGet != null) {
            if ( attrToGet instanceof SortedSet) {
                _attrToGet = (SortedSet<String>) attrToGet;
            } else {
                for (String toGet :  attrToGet) {
                    _attrToGet.add(toGet);
                }                            
            }
        }
        //append default attributes to get, needed to be always there
        _attrs = new HashMap<String, List<Object>>();
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
        if (!_attrToGet.contains(name)) {
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
            final Attribute attr = AttributeBuilder.build(entry.getKey(), entry.getValue());
            attrs.add(attr);
        }
        return attrs;
    }
}