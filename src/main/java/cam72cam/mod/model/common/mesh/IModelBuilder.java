package cam72cam.mod.model.common.mesh;

import cam72cam.mod.model.common.material.Material;
import cam72cam.mod.model.common.material.TextureRepacker;
import cam72cam.mod.resource.Identifier;

import java.io.IOException;
import java.io.InputStream;

public interface IModelBuilder {
    void newModelGroup(String name);

    void setCurrentMaterial(Material material);

    int addIndexedVert(float x, float y, float z);
    int addIndexedUv(float u, float v);
    int addIndexedNormal(float nx, float ny, float nz);

    IFaceBuilder newFace();

    void doSmoothShading();

	void finish();
    boolean isFinished();

    Model build(VAOLayout layout);

    Identifier getModelLoc();
    TextureRepacker getRepacker();

    /**
     * Open a resource via Builder, letting they handle cache things
     * @param id The resource/cache key to open
     * @return The opened stream
     */
    default InputStream open(Identifier id) throws IOException {
        return id.getLastResourceStream();
    }

    default void checkUnfinished() {
        if (isFinished()) {
            throw new IllegalStateException("Model builder already finished");
        }
    }
    default void checkFinished() {
        if (!isFinished()) {
            throw new IllegalStateException("Must call finish() before this method");
        }
    }

    interface IFaceBuilder {
        IFaceBuilder addVert(int posIdx, int uvIdx, int normalIdx);

        void end();
    }
}
