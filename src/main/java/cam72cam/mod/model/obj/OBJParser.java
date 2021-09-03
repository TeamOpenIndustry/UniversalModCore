package cam72cam.mod.model.obj;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.Buffers.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

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
    private boolean smoothShading = false;

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
                        if (args.length == 2 && args[1].equals("1")) {
                            // Technically this should be for every group, but this is a close enough approximation
                            this.smoothShading = true;
                        }
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
            Set<Vec3d> points = new HashSet<>();
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
                    Vec3d pt = new Vec3d(x, y, z);
                    points.add(pt);

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
                }
                faceCount++;
            }


            Vec3d first = points.stream().findFirst().orElse(Vec3d.ZERO);
            Vec3d groupMin = points.stream().reduce(first, Vec3d::min);
            Vec3d groupMax = points.stream().reduce(first, Vec3d::max);
            Vec3d center = groupMax.add(groupMin).scale(0.5);

            Vec3d min = first;
            Vec3d max = first;
            // Furthest from center
            for (Vec3d point : points) {
                if (max.distanceToSquared(center) < point.distanceToSquared(center)) {
                    max = point;
                }
            }
            //
            for (Vec3d point : points) {
                if (min.distanceToSquared(max) < point.distanceToSquared(max)) {
                    min = point;
                }
            }
            Vec3d finalMin = min.lengthSquared() < max.lengthSquared() ? min : max;
            Vec3d finalMax = min.lengthSquared() < max.lengthSquared() ? max : min;
            List<Vec3d> minG = points.stream().filter(p -> p.distanceToSquared(finalMin) < p.distanceToSquared(finalMax)).collect(Collectors.toList());
            List<Vec3d> maxG = points.stream().filter(p -> p.distanceToSquared(finalMin) > p.distanceToSquared(finalMax)).collect(Collectors.toList());
            Vec3d minN = minG.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1. / minG.size());
            Vec3d maxN = maxG.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1. / maxG.size());
            Vec3d normal = maxN.subtract(minN).normalize();

            correctedGroups.add(new OBJGroup(group.name, startFace, faceCount-1, groupMin, groupMax, normal));
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
    public boolean isSmoothShading() {
        return smoothShading;
    }

    private void addMaterialLibrary(String lib) {
        materialLibraries.add(lib);
    }

    private void setCurrentMTL(String name) {
        currentMaterial = name.intern();
    }

    private void addGroup(String name) {
        if (currentGroupStart != faceMaterials.size()) {
            groups.add(new OBJGroup(currentGroupName, currentGroupStart, faceMaterials.size() - 1, null, null, null));
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
