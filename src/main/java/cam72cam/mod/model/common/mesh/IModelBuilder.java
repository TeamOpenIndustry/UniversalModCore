package cam72cam.mod.model.common.mesh;

import cam72cam.mod.model.common.material.Material;
import cam72cam.mod.resource.Identifier;

import java.io.IOException;
import java.io.InputStream;

/**
 * Builds a {@link Model} from parsed geometry.
 */
public interface IModelBuilder {
    /**
     * @return The location of the model being built
     * */
    Identifier getModelLoc();

    /**
     * Starts a new named group, which contains all faces until next <code>newModelGroup()</code> call.
     * @param name The group name
     */
    void newModelGroup(String name);

    /**
     * Sets the material applied to subsequently added faces.
     * @param material The material to use
     */
    void setCurrentMaterial(Material material);

    /**
     * Adds a position and returns its index for use in {@link #newFace()}.
     * @return The index of the added position
     */
    int addIndexedVert(float x, float y, float z);
    /**
     * Adds a UV coord and returns its index for use in {@link #newFace()}.
     * @return The index of the added UV
     */
    int addIndexedUv(float u, float v);
    /**
     * Adds a normal and returns its index for use in {@link #newFace()}.
     * @return The index of the added normal
     */
    int addIndexedNormal(float nx, float ny, float nz);

    /**
     * Starts a new face.
     * @return A builder for the new face
     */
    IFaceBuilder newFace();

    /**
     * Marks the model as using smooth shading.
     */
    void doSmoothShading();

    /**
     * Opens a resource.<br>
     * The builder will return the cached one if possible, aiming at speeding up the load routine.
     * @param id The resource to open
     * @return The opened stream
     */
    default InputStream open(Identifier id) throws IOException {
        return id.getLastResourceStream();
    }

    /** Incrementally builds a single face from vertex references. */
    interface IFaceBuilder {
        /**
         * Adds a vertex reference to the face.
         * @param posIdx    Index of a vertex previously added via {@link IModelBuilder#addIndexedVert}
         * @param uvIdx     Index of a UV previously added via {@link IModelBuilder#addIndexedUv}, or -1 if not present
         * @param normalIdx Index of a normal previously added via {@link IModelBuilder#addIndexedNormal}, or -1 if not present
         * @return this face builder
         */
        IFaceBuilder addVert(int posIdx, int uvIdx, int normalIdx);

        /** Finalizes the face for the builder to do post editions, like triangulation */
        void end();
    }
}
