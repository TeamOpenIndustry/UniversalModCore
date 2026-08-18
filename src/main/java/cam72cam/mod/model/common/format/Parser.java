package cam72cam.mod.model.common.format;

import cam72cam.mod.model.common.mesh.IModelBuilder;

import java.io.IOException;

/**
 * A model format parser.
 */
@FunctionalInterface
public interface Parser {
	/**
	 * Parses the model referenced by {@link IModelBuilder#getModelLoc()} into the builder.
	 * @param builder The builder to emit geometry/materials/groups into
	 */
	void parse(final IModelBuilder builder) throws IOException;
}
