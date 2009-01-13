package org.identityconnectors.solaris;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;

public class SolarisHelper {

    public static String getPassword(SolarisConfiguration config2) {
        // TODO debug if password is visible
        char[] cleanPasswd = null;
        GuardedStringAccessor gsa;
        try {
            GuardedString pass = config2.getPassword();
            gsa = new GuardedStringAccessor();
            pass.access(gsa);
            cleanPasswd = gsa.getArray();
            return new String(cleanPasswd);
        } finally {
            if (cleanPasswd != null) {
                SecurityUtil.clear(cleanPasswd);
            }
        }
    }

}
