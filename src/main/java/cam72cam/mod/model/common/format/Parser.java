package cam72cam.mod.model.common.format;

import cam72cam.mod.model.common.mesh.IModelBuilder;
import cam72cam.mod.resource.Identifier;

@FunctionalInterface
public interface Parser {
	void parse(Identifier modelLoc, IModelBuilder builder);
}
