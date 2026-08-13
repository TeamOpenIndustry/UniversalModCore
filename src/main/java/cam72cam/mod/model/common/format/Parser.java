package cam72cam.mod.model.common.format;

import cam72cam.mod.model.common.mesh.IModelBuilder;

@FunctionalInterface
public interface Parser {
	void parse(final IModelBuilder builder);
}
