package cam72cam.mod.loader;

import cam72cam.mod.ModCore;
import com.google.gson.*;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.VersionParser;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

public class UMCModParser {
    private static final JsonParser parser = new JsonParser();

    public static UMCModContainer parse(ModCandidate candidate, InputStream stream) throws NoSuchElementException {
        return parse(candidate, stream, null);
    }

    public static UMCModContainer parse(ModCandidate candidate, InputStream stream, File resources) throws NoSuchElementException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            JsonElement root = parser.parse(reader);
            JsonObject obj = root.getAsJsonObject();

            UMCModContainer container = new UMCModContainer();

            container.candidate = candidate;
            container.source = candidate.getModContainer();
            container.modId = getString(obj, "modId").orElseThrow(form(candidate, "modId"));
            container.displayName = getString(obj, "displayName").orElseThrow(form(candidate, "displayName"));
            container.mainClass = getString(obj, "mainClass").orElseThrow(form(candidate, "mainClass"));
            container.version = getString(obj, "version").orElseThrow(form(candidate, "version"));
            String modURL = getString(obj, "modURL").orElse("");
            if (!modURL.isEmpty()) {
                container.modURL = new URL(modURL);
            }
            container.license = getString(obj, "license").orElse("N/A");
            container.resourcesRoot = resources;

            container.authors = new ArrayList<>();
            if (obj.has("authors") && obj.get("authors").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("authors");
                for (JsonElement e : arr) {
                    container.authors.add(e.getAsString());
                }
            }

            container.description = new ArrayList<>();
            if (obj.has("description") && obj.get("description").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("description");
                for (JsonElement e : arr) {
                    container.description.add(e.getAsString());
                }
            }

            container.dependencies = new HashMap<>();
            container.dependencies.put(ModCore.MODID, VersionParser.parseVersionReference(ModCore.MODID + "@" + "[1.2,1.3)"));
            if (obj.has("dependencies") && obj.get("dependencies").isJsonObject()) {
                JsonObject depsObj = obj.getAsJsonObject("dependencies");
                String[] keys = new String[]{"default", ModCore.semanticVersion()};
                for (String key : keys) {
                    if (depsObj.has(key)) {
                        JsonArray value = depsObj.getAsJsonArray(key);
                        for (JsonElement depElem : value.getAsJsonArray()) {
                            JsonObject depObj = depElem.getAsJsonObject();
                            ArtifactVersion artifact = VersionParser.parseVersionReference(
                                    String.format("%s@%s",
                                                  getString(depObj, "modId").orElseThrow(form(candidate, "dependencies.modId")),
                                                  getString(depObj, "versionRange").orElse("[,]")));
                            container.dependencies.put(artifact.getLabel(), artifact);
                        }
                    }
                }
            }

            container.bindMetadata(null);
            return container;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse UMC mod", e);
        }
    }

    private static Optional<String> getString(JsonObject obj, String member) {
        if (obj.has(member) && obj.get(member).isJsonPrimitive()) {
            return Optional.of(obj.get(member).getAsString());
        }
        return Optional.empty();
    }

    private static Supplier<NoSuchElementException> form(ModCandidate source, String field) {
        return () -> new NoSuchElementException(String.format("Failed to get mandatory field '%s' in UMC mod %s", field, source.getModContainer().getName()));
    }
}