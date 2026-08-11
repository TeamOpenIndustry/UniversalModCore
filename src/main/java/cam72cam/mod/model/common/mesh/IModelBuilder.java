package cam72cam.mod.model.common.mesh;

import cam72cam.mod.model.common.material.Material;

public interface IModelBuilder {
    void newModelGroup(String name);

    void setCurrentMaterial(Material material);

    int addIndexedVert(float x, float y, float z);
    int addIndexedUv(float u, float v);
    int addIndexedNormal(float nx, float ny, float nz);

    IFaceBuilder face();

	void finish();

    interface IFaceBuilder {
        //Tracked by builder
        IFaceBuilder vert(int posIdx, int uvIdx, int normalIdx);

        void end();
    }
}
