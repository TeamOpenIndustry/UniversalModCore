package cam72cam.mod.model.common.parser;

import cam72cam.mod.model.common.Model;
import cam72cam.mod.resource.Identifier;

import java.io.IOException;

public interface IModelParser {
    Model parse(Identifier location) throws IOException;
}