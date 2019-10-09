package cam72cam.mod.text;

import cam72cam.mod.resource.Identifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class CustomTranslations {
    private static Map<String, String> translations = null;
    public static Map<String, String> getTranslations() {
        if (translations == null) {
            try {
                InputStream input = CustomTranslations.class.getResourceAsStream("/assets/immersiverailroading/lang/en_US.lang");
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                translations = new HashMap<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] splits = line.split("=", 2);
                    if (splits.length == 2) {
                        translations.put(splits[0], splits[1]);
                    }
                }
                reader.close();
                input.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return translations;
    }


}
