/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.framework.common.objects;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.FrameworkUtil;

/**
 * Abstract "place-holder" for synchronization. The application must not make
 * any attempt to interpret the value of the token. From the standpoint of the
 * application the token is merely a black-box. The application may only persist
 * the value of the token for use on subsequent synchronization requests.
 * <p>
 * What this token represents is entirely connector-specific. On some connectors
 * this might be a last-modified value. On others, it might be a unique ID of a
 * log table entry.
 */
public final class SyncToken {

    private Object _value;

    /**
     * Creates a new
     * 
     * @param value
     *            May not be null. TODO: define set of allowed value types
     *            (currently same as set of allowed attribute values).
     */
    public SyncToken(Object value) {
        Assertions.nullCheck(value, "value");
        FrameworkUtil.checkAttributeValue(value);
        _value = value;
    }

    /**
     * Returns the value for the token.
     * 
     * @return The value for the token.
     */
    public Object getValue() {
        return _value;
    }
    
    @Override
    public String toString() {
        return "SyncToken: " + _value.toString();
    }
    
    @Override
    public int hashCode() {
        return _value.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if ( o instanceof SyncToken ) {
            SyncToken other = (SyncToken)o;
            return CollectionUtil.equals(_value, other._value);
        }
        return false;
    }


}
