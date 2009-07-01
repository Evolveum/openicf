package org.identityconnectors.racf;

import java.util.Arrays;

import org.identityconnectors.common.security.GuardedString;

public class GuardedStringAccessor implements GuardedString.Accessor {
    private char[] _array;
    
    public void access(char[] clearChars) {
        _array = new char[clearChars.length];
        System.arraycopy(clearChars, 0, _array, 0, _array.length);
    }
    
    public char[] getArray() {
        return _array;
    }

    public void clear() {
        Arrays.fill(_array, 0, _array.length, ' ');
    }
}