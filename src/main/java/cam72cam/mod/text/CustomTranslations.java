package cam72cam.mod.text;

import cam72cam.mod.ModCore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.LanguageDefinition;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CustomTranslations {
    private static Map<String, Map<String, String>> translations = new HashMap<>();
    public static Map<String, String> getTranslations() {
        if (ModCore.modIDs().isEmpty()) {
            return Collections.emptyMap();
        }
        LanguageDefinition lango = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        if (lango == null) {
            // still in init
            return Collections.EMPTY_MAP;
        }
        String lang = lango.getCode();
        if (!translations.containsKey(lang)) {
            Map<String, String> lt = new HashMap<>();
            translations.put(lang, lt);
            for (String modID : ModCore.modIDs()) {
                try {
                    String langStr = lang.split("_")[0] + "_" + lang.split("_")[1].toUpperCase();
                    InputStream input = CustomTranslations.class.getResourceAsStream("/assets/" + modID + "/lang/" + langStr + ".lang");
                    if (input == null) {
                        return Collections.emptyMap();
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] splits = line.split("=", 2);
                        if (splits.length == 2) {
                            lt.put(splits[0], splits[1]);
                        }
                    }
                    reader.close();
                    input.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return translations.get(lang);
    }


}
