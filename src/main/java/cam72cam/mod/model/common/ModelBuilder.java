package cam72cam.mod.model.common;

import cam72cam.mod.model.common.Buffers.FloatBuffer;
import cam72cam.mod.model.common.Buffers.IntBuffer;
import cam72cam.mod.model.common.opengl.VertexBuffer;

import java.util.*;

//TODO datatype other than triangle
public class ModelBuilder {
    private final VertexAttribute vertexAttribute;

    private final float[] current = new float[12];
    private final boolean[] hasProperty = new boolean[12];

    //Internal mapping
    private static final int pos = 0;
    private static final int col = 3;
    private static final int norm = 7;
    private static final int tex = 10;

    private final FloatBuffer data;           //vertexAttribute.stride floats per vert
    private final IntBuffer materialsMapping; //1 float per vert
    private boolean isBuilding;
    private int verticesCount;
    private int currentMaterial = 0;
    private final List<Material> usedMaterials;

    private ModelBuilder(VertexAttribute vertexAttribute) {
        this.vertexAttribute = vertexAttribute;
        this.data = new FloatBuffer(1024);
        this.materialsMapping = new IntBuffer(1024);
        usedMaterials = new ArrayList<>();
        usedMaterials.add(Material.UNDEFINED); //Add as 0# element
    }

    public static ModelBuilder start(VertexAttribute vertexAttribute) {
        return new ModelBuilder(vertexAttribute);
    }

    public ModelBuilder addVertex(float x, float y, float z) {
        if (!isBuilding) {
            isBuilding = true;
        }

        Arrays.fill(current, 0);
        Arrays.fill(hasProperty, false);
        current[pos] = x;
        current[pos + 1] = y;
        current[pos + 2] = z;
        hasProperty[pos] = true;
        return this;
    }

    public ModelBuilder color(float r, float g, float b, float a) {
        current[col] = r;
        current[col + 1] = g;
        current[col + 2] = b;
        current[col + 3] = a;
        hasProperty[col] = true;
        return this;
    }

    public ModelBuilder normal(float nx, float ny, float nz) {
        current[norm]  = nx;
        current[norm + 1] = ny;
        current[norm + 2] = nz;
        hasProperty[norm] = true;
        return this;
    }

    public ModelBuilder tex(float u, float v) {
        current[tex] = u;
        current[tex + 1] = v;
        hasProperty[tex] = true;
        return this;
    }

    public ModelBuilder endVert() {
        for (VertexAttribute.Elements elements : vertexAttribute.getElements().keySet()) {
            switch (elements) {
                case POSITION:
                    if (!hasProperty[pos]) {
                        throw new IllegalStateException("Incompleted vertex!");
                    }
                    data.add(current[pos], current[pos + 1], current[pos + 2]);
                    break;
                case COLOR:
                    if (!hasProperty[col]) {
                        throw new IllegalStateException("Incompleted vertex!");
                    }
                    data.add(current[col], current[col + 1], current[col + 2], current[col + 3]);
                    break;
                case NORMAL:
                    if (!hasProperty[norm]) {
                        throw new IllegalStateException("Incompleted vertex!");
                    }
                    data.add(current[norm], current[norm + 1], current[norm + 2]);
                    break;
                case TEXTURE_UV:
                    if (!hasProperty[tex]) {
                        throw new IllegalStateException("Incompleted vertex!");
                    }
                    data.add(current[tex], current[tex + 1]);
                    break;
            }
        }
        materialsMapping.add(currentMaterial);

        if (verticesCount == 2) {
            isBuilding = false;
            verticesCount = 0;
        }
        return this;
    }

    public ModelBuilder setMaterial(Material material) {
        if (isBuilding)
            throw new IllegalStateException("Cannot set material during building!");
        if (!usedMaterials.contains(material)) {
            usedMaterials.add(material);
        }
        currentMaterial = usedMaterials.indexOf(material);
        return this;
    }

    public Model build() {
        if(isBuilding)
            throw new IllegalStateException("Cannot finish Model with unfinished face!");
        Geometry geom = new Geometry(vertexAttribute, new VertexBuffer(data.array()));
        return new Model(geom, new HashSet<>(usedMaterials));
    }
}
