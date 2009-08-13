package org.identityconnectors.vms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class VmsUtilities {

    public static String readFileFromClassPath(String fileName) throws IOException {
        ClassLoader cl = null;
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        StringBuffer buf = new StringBuffer();

        try {
            cl = VmsUtilities.class.getClassLoader();
            is = cl.getResourceAsStream(fileName);

            if (is != null) {
                isr = new InputStreamReader(is);
                br = new BufferedReader(isr);
                String s = null;
                while ((s = br.readLine()) != null) {
                    buf.append(s);
                    buf.append("\n");
                }
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                }
            } else if (isr != null) {
                try {
                    isr.close();
                } catch (Exception e) {
                }
            } else if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }

        return buf.toString();
    }

}
