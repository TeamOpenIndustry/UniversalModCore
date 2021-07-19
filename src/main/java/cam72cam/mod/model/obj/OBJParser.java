package cam72cam.mod.model.obj;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.Buffers.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OBJParser {
    public static final float UNSPECIFIED = Float.MIN_VALUE;

    // 3 floats per vertex (x, y, z)
    private final FloatBuffer vertices = new FloatBuffer(1024);
    // 3 floats per normal (x, y, z)
    private final FloatBuffer vertexNormals = new FloatBuffer(1024);
    // 2 floats per normal (u, v)
    private final FloatBuffer vertexTextures = new FloatBuffer(1024);
    // 3 ints per vert, 3 verts per face (v1, vt1, vn1, v2, vt2, vn2, v3, vt3, vn3)
    private final IntBuffer faceVerts = new IntBuffer(1024);
    // 1 int per face, which face to use (mtlLookup)
    private final List<String> faceMaterials = new ArrayList<>();
    // List of material files to load as part of this obj
    private final List<String> materialLibraries = new ArrayList<>();
    // Group -> Face # ranges
    private final List<OBJGroup> groups = new ArrayList<>();

    private final float scale;

    private final List<OBJGroup> correctedGroups;
    private final VertexBuffer buffer;
    private final String[] correctedFaceMaterials;

    private String currentMaterial = null;
    private int currentGroupStart = 0;
    private String currentGroupName = "defaultName";
    private boolean hasNormals = true;

    public OBJParser(InputStream stream, float scale) throws IOException {
        this.scale = scale;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.length() == 0) {
                    continue;
                }
                String[] args = line.split(" ");
                String cmd = args[0];
                switch (cmd) {
                    case "mtllib":
                        addMaterialLibrary(args[1]);
                        break;
                    case "usemtl":
                        String mtlName = args[1];
                        for (int i = 2; i < args.length; i++) {
                            mtlName += " " + args[i];
                        }
                        setCurrentMTL(mtlName);
                        break;
                    case "o":
                    case "g":
                        addGroup(args[1]);
                        break;
                    case "v":
                        addVertex(args[1], args[2], args[3]);
                        break;
                    case "vn":
                        addVertexNormal(args[1], args[2], args[3]);
                        break;
                    case "vt":
                        addVertexTexture(args[1], args[2]);
                        break;
                    case "f":
                        if (args.length == 4) {
                            addFace(args[1], args[2], args[3]);
                        } else if (args.length == 5) {
                            addFace(args[1], args[2], args[3]);
                            addFace(args[3], args[4], args[1]);
                        } else {
                            for (int i = 2; i < args.length - 1; i++) {
                                addFace(args[1], args[i], args[i + 1]);
                            }
                        }
                        break;
                    case "s":
                        //Ignore
                        break;
                    case "l":
                        // Ignore
                        // TODO might be able to use this for details
                        break;
                    default:
                        //System.out.println("OBJ: ignored line '" + line + "'");
                        break;
                }
            }
            addGroup(null); // Finalize last group
        }

        groups.sort(Comparator.comparing(a -> a.name));
        this.correctedGroups = new ArrayList<>();

        float[] vertices = this.vertices.array();
        float[] vertexNormals = this.vertexNormals.array();
        float[] vertexTextures = this.vertexTextures.array();
        int[] faceVerts = this.faceVerts.array();
        this.correctedFaceMaterials = new String[faceMaterials.size()];

        this.buffer = new VertexBuffer(faceMaterials.size(), hasNormals);

        int faceCount = 0;
        int vertexOffset = buffer.vertexOffset;
        int normalOffset = buffer.normalOffset;
        int textureOffset = buffer.textureOffset;

        for (OBJGroup group : groups) {
            int startFace = faceCount;
            float minX = 0;
            float minY = 0;
            float minZ = 0;
            float maxX = 0;
            float maxY = 0;
            float maxZ = 0;
            for (int face = group.faceStart; face <= group.faceStop; face++) {
                correctedFaceMaterials[faceCount] = faceMaterials.get(face);
                for (int point = 0; point < 3; point++) {
                    int faceVertexIdx = face * 3 * 3 + point * 3;

                    int vertex = faceVerts[faceVertexIdx+0] * 3;
                    float x = vertices[vertex+0];
                    float y = vertices[vertex+1];
                    float z = vertices[vertex+2];
                    buffer.data[vertexOffset+0] = x;
                    buffer.data[vertexOffset+1] = y;
                    buffer.data[vertexOffset+2] = z;
                    vertexOffset += buffer.stride;

                    int texture = faceVerts[faceVertexIdx+1] * 2;
                    if (texture >= 0) {
                        buffer.data[textureOffset+0] = vertexTextures[texture+0];
                        buffer.data[textureOffset+1] = vertexTextures[texture+1];
                    } else {
                        buffer.data[textureOffset+0] = UNSPECIFIED;
                        buffer.data[textureOffset+1] = UNSPECIFIED;
                    }
                    textureOffset += buffer.stride;

                    if (hasNormals) {
                        int normal = faceVerts[faceVertexIdx+2] * 3;
                        buffer.data[normalOffset+0] = vertexNormals[normal+0];
                        buffer.data[normalOffset+1] = vertexNormals[normal+1];
                        buffer.data[normalOffset+2] = vertexNormals[normal+2];
                        normalOffset += buffer.stride;
                    }

                    if (point == 0 && faceCount == startFace) {
                        minX = maxX = x;
                        minY = maxY = y;
                        minZ = maxZ = z;
                    } else {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        maxZ = Math.max(maxZ, z);
                    }
                }
                faceCount++;
            }
            correctedGroups.add(new OBJGroup(group.name, startFace, faceCount-1, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ)));
        }
    }
    public VertexBuffer getBuffer() {
        return buffer;
    }
    public List<OBJGroup> getGroups() {
        return correctedGroups;
    }
    public List<String> getMaterialLibraries() {
        return materialLibraries;
    }
    public String[] getFaceMaterials() {
        return correctedFaceMaterials;
    }

    private void addMaterialLibrary(String lib) {
        materialLibraries.add(lib);
    }

    private void setCurrentMTL(String name) {
        currentMaterial = name.intern();
    }

    private void addGroup(String name) {
        if (currentGroupStart != faceMaterials.size()) {
            groups.add(new OBJGroup(currentGroupName, currentGroupStart, faceMaterials.size() - 1, null, null));
        }
        currentGroupName = name;
        currentGroupStart = faceMaterials.size();
    }

    private void addVertex(String x, String y, String z) {
        vertices.add(Float.parseFloat(x) * scale);
        vertices.add(Float.parseFloat(y) * scale);
        vertices.add(Float.parseFloat(z) * scale);
    }
    private void addVertexTexture(String u, String v) {
        vertexTextures.add(Float.parseFloat(u));
        vertexTextures.add(Float.parseFloat(v));
    }
    private void addVertexNormal(String x, String y, String z) {
        vertexNormals.add(Float.parseFloat(x));
        vertexNormals.add(Float.parseFloat(y));
        vertexNormals.add(Float.parseFloat(z));
    }

    private void addFace(String a, String b, String c) {
        parsePoint(a);
        parsePoint(b);
        parsePoint(c);
        faceMaterials.add(currentMaterial);
    }

    private void parsePoint(String point) {
        String[] sp = point.split("/");
        for (int i = 0; i < 3; i++) {
            if (i < sp.length && !sp[i].equals("")) {
                faceVerts.add(Integer.parseInt(sp[i]) - 1);
            } else {
                faceVerts.add(-1);
                if (i == 2) {
                    //VN
                    this.hasNormals = false;
                }
            }
        }
    }
}
