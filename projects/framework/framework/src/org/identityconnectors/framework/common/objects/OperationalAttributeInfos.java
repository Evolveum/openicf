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


import java.util.EnumSet;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;

/**
 * {@link AttributeInfo} for each operational attribute.
 */
public final class OperationalAttributeInfos {
    /**
     * Gets/sets the enable status of an object.
     */
    public static final AttributeInfo ENABLE = AttributeInfoBuilder.build(
            OperationalAttributes.ENABLE_NAME, boolean.class);

    /**
     * Gets/sets the enable date for an object.
     */
    public static final AttributeInfo ENABLE_DATE = AttributeInfoBuilder.build(
            OperationalAttributes.ENABLE_DATE_NAME, long.class);

    /**
     * Gets/sets the disable date for an object.
     */
    public static final AttributeInfo DISABLE_DATE = AttributeInfoBuilder.build(
            OperationalAttributes.DISABLE_DATE_NAME, long.class);

    /**
     * Gets/sets the lock out attribute for an object.
     */
    public static final AttributeInfo LOCK_OUT = AttributeInfoBuilder.build(
            OperationalAttributes.LOCK_OUT_NAME, boolean.class);

    /**
     * Gets/sets the password expiration date for an object.
     */
    public static final AttributeInfo PASSWORD_EXPIRATION_DATE = AttributeInfoBuilder.build(
            OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, long.class);

    /**
     * Normally this is a write-only attribute. Sets the password for an object.
     */
    public static final AttributeInfo PASSWORD = AttributeInfoBuilder.build(
            OperationalAttributes.PASSWORD_NAME, GuardedString.class,
            EnumSet.of(Flags.NOT_READABLE,Flags.NOT_RETURNED_BY_DEFAULT));
    
    /**
     * Used in conjunction with password to do an account level password change.
     * This is for a non-administrator change of the password and therefore
     * requires the current password.
     */
    public static final AttributeInfo CURRENT_PASSWORD = AttributeInfoBuilder.build(
            OperationalAttributes.CURRENT_PASSWORD_NAME, GuardedString.class,
            EnumSet.of(Flags.NOT_READABLE,Flags.NOT_RETURNED_BY_DEFAULT));

    /**
     * Used to do an administrator reset of the password. The value is the reset
     * password value.
     */
    public static final AttributeInfo RESET_PASSWORD = AttributeInfoBuilder.build(
            OperationalAttributes.RESET_PASSWORD_NAME, GuardedString.class,
            EnumSet.of(Flags.NOT_READABLE,Flags.NOT_RETURNED_BY_DEFAULT));
    
    
    /**
     * Used to determine if a password is expired or to expire a password.
     */
    public static final AttributeInfo PASSWORD_EXPIRED = AttributeInfoBuilder.build(
            OperationalAttributes.PASSWORD_EXPIRED_NAME, boolean.class);

}
