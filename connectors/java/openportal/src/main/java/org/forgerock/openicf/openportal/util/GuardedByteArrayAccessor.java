/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.forgerock.openicf.openportal.util;

/**
 *
 * @author admin
 */
import java.util.Arrays;
import org.identityconnectors.common.security.GuardedByteArray;

public class GuardedByteArrayAccessor implements GuardedByteArray.Accessor {

    public static final String code_id = "$Id$";
    private byte[] array;

    public void access(byte[] clearBytes) {
        array = new byte[clearBytes.length];
        System.arraycopy(clearBytes, 0, array, 0, array.length);
    }

    public byte[] getArray() {
        return array;
    }

    public void clear() {
        Arrays.fill(array, Byte.MIN_VALUE);
    }
}