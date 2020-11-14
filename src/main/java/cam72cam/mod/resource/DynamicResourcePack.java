package cam72cam.mod.resource;

import cam72cam.mod.ModCore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DynamicResourcePack implements ResourcePack {
    public static final DynamicResourcePack INSTANCE = new DynamicResourcePack();

    private static final Map<Identifier, Supplier<String>> resources = new HashMap<>();

    static {
        for (LanguageDefinition language : MinecraftClient.getInstance().getLanguageManager().getAllLanguages()) {
            String lang = language.getCode();
            for (String modID : ModCore.modIDs()) {
                if (!lang.contains("_")) {
                    // This is probably wrong...
                    continue;
                }
                String langStr = lang.split("_")[0] + "_" + lang.split("_")[1].toUpperCase();
                try (InputStream input = DynamicResourcePack.class.getResourceAsStream("/assets/" + modID + "/lang/" + langStr + ".lang")) {
                    if (input == null) {
                        continue;
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                    List<String> translations = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] splits = line.split("=", 2);
                        if (splits.length == 2) {
                            String key = splits[0];
                            String value = splits[1];

                            translations.add(String.format("\"%s\": \"%s\"", key, value));
                            translations.add(String.format("\"%s\": \"%s\"", key.replace(":", "."), value));
                            translations.add(String.format("\"%s\": \"%s\"", key.replace(".name", ""), value));
                            translations.add(String.format("\"%s\": \"%s\"", key.replace(".name", "").replace(":", "."), value));
                        }
                    }
                    addResource(new Identifier(modID, String.format("lang/%s.json", lang)), "{" + String.join(",", translations) + "}");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    public static void addResource(Identifier id, String value) {
        addResource(id, () -> value);
    }

    public static void addResource(Identifier id, Supplier<String> value) {
        resources.put(id, value);
    }

    private DynamicResourcePack() {

    }

    @Override
    public InputStream openRoot(String fileName) throws IOException {
        return null;
    }

    @Override
    public InputStream open(ResourceType type, Identifier id) throws IOException {
        return new ByteArrayInputStream(resources.get(id).get().getBytes());
    }

    @Override
    public Collection<Identifier> findResources(ResourceType type, String namespace, int maxDepth, Predicate<String> pathFilter) {
        return resources.keySet().stream().filter(f -> f.getNamespace().equals(namespace) && pathFilter.test(f.getPath())).collect(Collectors.toSet());
    }

    @Override
    public boolean contains(ResourceType type, Identifier id) {
        return resources.containsKey(id);
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        return resources.keySet().stream().map(Identifier::getNamespace).collect(Collectors.toSet());
    }

    @Nullable
    @Override
    public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
        return null;
    }

    @Override
    public String getName() {
        return "Universal Mod Core Dynamic Resources";
    }

    @Override
    public void close() throws IOException {

    }
}
