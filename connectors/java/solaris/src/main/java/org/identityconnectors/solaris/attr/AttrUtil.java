/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package org.identityconnectors.solaris.attr;

import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

/**
 * @author Radovan Semancik
 *
 */
public final class AttrUtil {

    private AttrUtil() {
    }

    public static AttributeInfo convertAccountSunAttrToAttrInfo(boolean sunCompat,
            AccountAttribute origAttr) {
        if (sunCompat) {
            return null;
        }
        if (origAttr == AccountAttribute.LOCK) {
            return AttributeInfoBuilder.build(OperationalAttributes.ENABLE_NAME, boolean.class);
        }
        return null;
    }

    public static String convertAccountIcfAttrToSun(boolean sunCompat, String icfAttrName) {
        String sunAttrName = icfAttrName;
        if (!sunCompat) {
            if (icfAttrName.equals(OperationalAttributes.ENABLE_NAME)) {
                sunAttrName = AccountAttribute.LOCK.getName();
            }
        }
        return sunAttrName;
    }

    public static NativeAttribute convertAccountIcfAttrToNative(boolean sunCompat,
            String icfAttrName) {
        return AccountAttribute
                .forAttributeName(convertAccountIcfAttrToSun(sunCompat, icfAttrName)).getNative();
    }

    public static Boolean parseBoolean(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            return Boolean.valueOf((String) val);
        } else {
            throw new IllegalArgumentException("Unexpected class " + val.getClass());
        }
    }

}
