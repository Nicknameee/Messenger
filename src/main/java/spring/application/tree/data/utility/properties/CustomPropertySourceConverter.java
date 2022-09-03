package spring.application.tree.data.utility.properties;

import java.util.HashMap;
import java.util.Map;

public class CustomPropertySourceConverter {
    /**
     * Serializing *.properties data to 'JSON' representation
     */
    public static Map<String , String> convertToKeyValueFormat(final String sourceContent) {
        Map<String , String> properties = new HashMap<>();
        String[] lines = sourceContent.split("\n");
        for (String line : lines) {
            String[] params = line.split("=");
            properties.put(params[0] , params[1]);
        }
        return properties;
    }
}