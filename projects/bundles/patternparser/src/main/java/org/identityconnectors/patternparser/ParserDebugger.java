package org.identityconnectors.patternparser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

public class ParserDebugger {
    
    public static void main(String[] args) {
        if (args.length!=2) {
            System.out.println("usage: ParserDebugger parserFile dataFile");
        }
        try {
            String parser = loadFromFile(args[0]);
            MapTransform transform = (MapTransform)Transform.newTransform(parser);
            transform.setDebug(true);
            String text = loadFromFile(args[1]);
            Map<String, Object> output = (Map<String, Object>)transform.transform(text);
            
            System.out.println("\nResults:");
            for (Map.Entry<String, Object> entry : output.entrySet()) {
                System.out.println("    "+entry.getKey()+"="+resultToString(entry.getValue()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static String resultToString(Object result) {
        if (result.getClass().isArray()) {
            return Arrays.deepToString((Object[])result);
        } else if (result == null) {
            return "<null>";
        } else {
            return result.toString();
        }
        
    }

    private static String loadFromFile(String fileName) throws IOException {
        BufferedReader is = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        StringBuffer data = new StringBuffer();
        String line = null;
        while ((line=is.readLine())!=null) {
            data.append(line+"\n");
        }
        return data.toString();
    }
}
