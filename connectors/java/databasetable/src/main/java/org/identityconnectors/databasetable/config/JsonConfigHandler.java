package org.identityconnectors.databasetable.config;

import org.identityconnectors.databasetable.DatabaseTableConfiguration;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class JsonConfigHandler {

    private Map<String, UniversalObjectClassHandler> universalObjectClassHandlers;

    public JsonConfigHandler(DatabaseTableConfiguration config) {
        this.universalObjectClassHandlers = new HashMap<>();
        this.parseJson(config);
    }

    public Map<String, UniversalObjectClassHandler> getUniversalObjectClassHandlers() { return this.universalObjectClassHandlers;}

    // Method for parsing a JSON file and creating a list of SchemaType objects
    public void parseJson(DatabaseTableConfiguration config) {
        try (FileReader fileReader = new FileReader(config.getJsonFilePath());
             JsonReader jsonReader = Json.createReader(fileReader)) {

            // Loading a JSON file and converting it to a JsonObject
            JsonObject jsonSchema = jsonReader.readObject();
            // Retrieving an array of objects from the file
            JsonArray objectsArray = jsonSchema.getJsonArray("objects");

            // Iterating through all objects in the array
            for (int i = 0; i < objectsArray.size(); i++) {
                UniversalObjectClassHandler universalObjectClassHandler = new UniversalObjectClassHandler();

                JsonObject object = objectsArray.getJsonObject(i);

                // Retrieving values from the current object
                universalObjectClassHandler.setConfig(config);
                universalObjectClassHandler.setObjectClassName(object.getString("objectClassName"));

                JsonObject configProperties = object.getJsonObject("configurationProperties");

                // Retrieving configuration property values from the new configuration object
                universalObjectClassHandler.setTable(configProperties.getString("Table"));
                universalObjectClassHandler.setKeyColumn(configProperties.getString("Key Column"));

                if (configProperties.containsKey("Enable writing empty string")) {
                    universalObjectClassHandler.setEnableWritingEmptyString(configProperties.getBoolean("Enable writing empty string"));
                }
                if (configProperties.containsKey("Change Log Column (Sync)")) {
                    universalObjectClassHandler.setChangeLogColumn(configProperties.getString("Change Log Column (Sync)"));
                }
                if (configProperties.containsKey("Sync Order Column")) {
                    universalObjectClassHandler.setSyncOrderColumn(configProperties.getString("Sync Order Column"));
                }
                if (configProperties.containsKey("Sync Order Asc")) {
                    universalObjectClassHandler.setSyncOrderAsc(configProperties.getBoolean("Sync Order Asc"));
                }
                if (configProperties.containsKey("Suppress Password")) {
                    universalObjectClassHandler.setSuppressPassword(configProperties.getBoolean("Suppress Password"));
                }
                // Retrieving optional configuration property values from the new configuration object
                if (configProperties.containsKey("All native")) {
                    universalObjectClassHandler.setSuppressPassword(configProperties.getBoolean("All native"));
                }
                if (configProperties.containsKey("Native Timestamps")) {
                    universalObjectClassHandler.setSuppressPassword(configProperties.getBoolean("Native Timestamps"));
                }

                // Creating an instance of SchemaTypeAttribute for each configuration property
                this.universalObjectClassHandlers.put(universalObjectClassHandler.getObjectClassName(), universalObjectClassHandler);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
