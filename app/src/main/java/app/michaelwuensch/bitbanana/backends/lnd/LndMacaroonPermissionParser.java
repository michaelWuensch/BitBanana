package app.michaelwuensch.bitbanana.backends.lnd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LndMacaroonPermissionParser {

    public static List<LndMacaroonPermission> parsePermissions(String data) {
        // Split the input string into lines
        String[] lines = data.split("\\n");

        // Map to store the permissions by identifier
        Map<String, LndMacaroonPermission> permissionsMap = new HashMap<>();

        // Process each line
        for (String line : lines) {
            // Check if the line contains "read" or "write"
            boolean hasRead = line.contains("read");
            boolean hasWrite = line.contains("write");

            if (hasRead || hasWrite) {
                // Extract the first word as the identifier
                String[] words = line.trim().split("\\W+");
                if (words.length > 0) {
                    String identifier = words[0];

                    // Initialize the Permission object if it doesn't exist
                    permissionsMap.putIfAbsent(identifier, new LndMacaroonPermission(identifier));
                    LndMacaroonPermission perm = permissionsMap.get(identifier);

                    // Set the appropriate boolean based on the line contents
                    if (hasRead) {
                        perm.canRead = true;
                    }
                    if (hasWrite) {
                        perm.canWrite = true;
                    }
                }
            }
        }

        // Convert the map values to a list
        return new ArrayList<>(permissionsMap.values());
    }
}