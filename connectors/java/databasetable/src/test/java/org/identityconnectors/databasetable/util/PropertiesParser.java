package org.identityconnectors.databasetable.util;

import org.identityconnectors.common.logging.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class PropertiesParser {

    private static final Log LOG = Log.getLog(PropertiesParser.class);
    private Properties properties;

    public PropertiesParser(String FilePath) throws IOException {

        InputStreamReader fileInputStream = new InputStreamReader(new FileInputStream(FilePath),
                StandardCharsets.UTF_8);
        properties = new Properties();
        properties.load(fileInputStream);
        fileInputStream.close();

    }

    public List<String> fetchTestData(String propertyName) {

        List<String> dataList = new ArrayList<>();

        for (Object propertyKey : properties.keySet()) {
            String keyValue = propertyKey.toString();
            if (!keyValue.contains(".")) {

                if (keyValue.equals(propertyName)) {

                    String value = properties.getProperty(keyValue);
                    LOG.ok("The name of the property which is being fetched: {0} and the value which was fetched: {1}", keyValue, value);
                    dataList.add(value);
                }

            } else {
                if (keyValue.contains(propertyName)) {

                    String value = properties.getProperty(keyValue);
                    LOG.ok("The name of the property which is being fetched: {0} and the value which was fetched: {1}", keyValue, value);
                    dataList.add(value);
                }
            }
        }

        return dataList;
    }

    public String fetchTestDataSingleValue(String propertyName) {

        return fetchTestData(propertyName).get(0);
    }

    public String[] fetchTestDataMultiValue(String propertyName) {

        List<String> propertyValues = fetchTestData(propertyName);

        String[] propertyArray = new String[propertyValues.size()];
        propertyArray = propertyValues.toArray(propertyArray);

        return propertyArray;
    }

}