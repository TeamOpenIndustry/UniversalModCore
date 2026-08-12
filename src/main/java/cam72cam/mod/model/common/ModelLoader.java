package cam72cam.mod.model.common;

import cam72cam.mod.model.common.format.OBJParser;
import cam72cam.mod.model.common.format.Parser;
import cam72cam.mod.model.common.mesh.GlModelBuilder;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.io.FilenameUtils;

import java.util.*;

public class ModelLoader {
    private static final Map<String, Parser> PARSER = new HashMap<>();

    public static void register(String extName, Parser parser) {
        PARSER.put(extName, parser);
    }

    public static Model load(Identifier model) {
        return load(model, 1f);
    }

    public static Model load(Identifier model, float scale) {
        return load(model, scale, Collections.emptySet());
    }

    public static Model load(Identifier model, float scale, Set<String> variants) {
        String extName = FilenameUtils.getExtension(model.getPath());
        if (!PARSER.containsKey(extName)) {
            throw new RuntimeException("Unknown extension: " + extName);
        }
        Parser parser = PARSER.get(extName);
        GlModelBuilder builder = new GlModelBuilder(scale, variants);
        parser.parse(model, builder);
        builder.finish();
        return builder.build(VAOLayout.POS_UV_COLOR_NORMAL);
    }

    static {
        PARSER.put("obj", OBJParser::parse);
    }
}
