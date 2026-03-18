package cam72cam.mod.model.common.parser;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.*;
import cam72cam.mod.model.common.opengl.VertexBuffer;
import cam72cam.mod.resource.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class OBJParser implements IModelParser {
    private Identifier model;
    private final Buffers.FloatBuffer positions = new Buffers.FloatBuffer(1024);
    private final Buffers.FloatBuffer normals = new Buffers.FloatBuffer(1024);
    private final Buffers.FloatBuffer textures = new Buffers.FloatBuffer(1024);
    // 3 ints per vert, 3 verts per face (v1, vt1, vn1, v2, vt2, vn2, v3, vt3, vn3)
    private final Buffers.IntBuffer indices = new Buffers.IntBuffer(1024);
    private final List<String> faceMaterials = new ArrayList<>();

    private final Map<String, Material> materials = new HashMap<>();
    private final List<ModelGroup> groups = new ArrayList<>();
    private final Map<String, ModelGroup> correctedGroups = new LinkedHashMap<>();
    private int currentGroupStart = 0;
    private String currentGroupName = "defaultName";
    private boolean hasNormals;

    @Override
    public Model parse(Identifier location) throws IOException {
        Material currentMaterial = Material.UNDEFINED;
        this.model = location;
        processData();

        float[] position = positions.array();
        float[] normal = normals.array();
        float[] texture = textures.array();
        int[] index = indices.array();

        VertexAttribute attribute = hasNormals ? VertexAttribute.POSITION_COLOR_NORMAL_UV : VertexAttribute.POSITION_COLOR_UV;
        int stride = attribute.getStride();
        float[] data = new float[(index.length / 3) * stride];

        //iter indices
        for (ModelGroup group : groups) {
            List<Vec3d> points = new ArrayList<>();
            for (int i = group.faceStart; i <= group.faceStop; i++) {
                for (int j = 0; j < 3; j++) {
                    int indicesPtr = i * 9 + j * 3;
                    int dataPtr = ((i + 1) / 3 + j) * stride;
                    for (Map.Entry<VertexAttribute.Elements, Integer> e : attribute.getElements().entrySet()) {
                        switch (e.getKey()) {
                            case POSITION:
                                int pos = index[indicesPtr] * 3;
                                data[dataPtr + e.getValue()] = position[pos];
                                data[dataPtr + e.getValue() + 1] = position[pos + 1];
                                data[dataPtr + e.getValue() + 2] = position[pos + 2];
                                Vec3d vec3d = new Vec3d(position[pos],  position[pos + 1], position[pos + 2]);
                                points.add(vec3d);
                                break;
                            case TEXTURE_UV:
                                int uv = index[indicesPtr + 1] * 2;
                                if (uv == -2) {
                                    data[dataPtr + e.getValue()] = 0;
                                    data[dataPtr + e.getValue() + 1] = 0;
                                } else {
                                    data[dataPtr + e.getValue()] = texture[uv];
                                    data[dataPtr + e.getValue() + 1] = texture[uv + 1];
                                }
                                break;
                            case NORMAL:
                                int norm = index[indicesPtr + 2] * 3;
                                data[dataPtr + e.getValue()] = normal[norm];
                                data[dataPtr + e.getValue() + 1] = normal[norm + 1];
                                data[dataPtr + e.getValue() + 2] = normal[norm + 2];
                                break;
                            case COLOR:
                                Material material = materials.get(faceMaterials.get((i + 1) / 9));
                                data[dataPtr + e.getValue()] = material.r;
                                data[dataPtr + e.getValue() + 1] = material.g;
                                data[dataPtr + e.getValue() + 2] = material.b;
                                data[dataPtr + e.getValue() + 3] = material.a;
                                break;
                        }
                    }
                }
            }

            Vec3d first = points.get(0);
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
            for (Vec3d point : points) {
                if (min.distanceToSquared(max) < point.distanceToSquared(max)) {
                    min = point;
                }
            }
            Vec3d finalMin = min.lengthSquared() < max.lengthSquared() ? min : max;
            Vec3d finalMax = min.lengthSquared() < max.lengthSquared() ? max : min;
            List<Vec3d> minG = points.stream().filter(p -> p.distanceToSquared(finalMin) < p.distanceToSquared(finalMax)).collect(
                    Collectors.toList());
            List<Vec3d> maxG = points.stream().filter(p -> p.distanceToSquared(finalMin) > p.distanceToSquared(finalMax)).collect(Collectors.toList());
            Vec3d minN = minG.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1. / minG.size());
            Vec3d maxN = maxG.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1. / maxG.size());
            Vec3d normalV = maxN.subtract(minN).normalize();

            correctedGroups.put(group.name, new ModelGroup(group.name, group.faceStart, group.faceStop, minN, maxN, normalV));
        }

        Geometry geometry = new Geometry(attribute, new VertexBuffer(data));
        return new Model(geometry, new HashSet<>(materials.values()), correctedGroups);
    }

    private void processData() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(model.getResourceStream()))) {
            String line;
            Material currentMaterial = Material.UNDEFINED;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                String[] args = line.split(" ");
                String cmd = args[0];
                switch (cmd) {
                    case "mtllib":
                        materials.putAll(parseMTL(model.getRelative(args[1])));
                        break;
                    case "usemtl":
                        if (args.length >= 2) {
                            StringBuilder mtlName = new StringBuilder(args[1]);
                            for (int i = 2; i < args.length; i++) {
                                mtlName.append(" ").append(args[i]);
                            }
                            currentMaterial = materials.getOrDefault(mtlName.toString(), Material.UNDEFINED);
                        } else {
                            currentMaterial = Material.UNDEFINED;
                        }
                        break;
                    case "o":
                    case "g":
                        StringBuilder groupName = new StringBuilder(args[1]);
                        for (int i = 2; i < args.length; i++) {
                            groupName.append(" ").append(args[i]);
                        }
                        addGroup(groupName.toString());
                        break;
                    case "v":
                        positions.add(Float.parseFloat(args[1]), Float.parseFloat(args[2]), Float.parseFloat(args[3]));
                        break;
                    case "vn":
                        normals.add(Float.parseFloat(args[1]), Float.parseFloat(args[2]), Float.parseFloat(args[3]));
                        break;
                    case "vt":
                        textures.add(Float.parseFloat(args[1]), Float.parseFloat(args[2]));
                        break;
                    case "f":
                        if (args.length == 4) {
                            addFace(args[1], args[2], args[3], currentMaterial.name);
                        } else if (args.length == 5) {
                            //TODO
                            addFace(args[1], args[2], args[3], currentMaterial.name);
                            addFace(args[3], args[4], args[1], currentMaterial.name);
                        } else {
                            for (int i = 2; i < args.length - 1; i++) {
                                addFace(args[1], args[i], args[i + 1], currentMaterial.name);
                            }
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Material> parseMTL(Identifier mtl) throws IOException {
        Map<String, Material> mat = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(mtl.getResourceStream()))) {
            String line;
            String materialName = null;
            Identifier map_Kd = null;
            Identifier map_Bump = null;
            Identifier map_Ns = null;
            float KdR = 1;
            float KdG = 1;
            float KdB = 1;
            float KdA = 1;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split(" ");
                switch (parts[0]) {
                    case "newmtl":
                        if (materialName != null) {
                            mat.put(materialName, new Material(materialName, map_Kd, map_Bump, map_Ns, KdR, KdG, KdB, KdA));
                        }
                        materialName = parts[1];
                        for (int i = 2; i < parts.length; i++) {
                            materialName += " " + parts[i];
                        }
                        map_Kd = null;
                        map_Ns = null;
                        map_Bump = null;
                        KdR = 1;
                        KdG = 1;
                        KdB = 1;
                        KdA = 1;
                        break;
                    case "Ka":
                        break;
                    case "Kd":
                        KdR = Float.parseFloat(parts[1]);
                        KdG = Float.parseFloat(parts[2]);
                        KdB = Float.parseFloat(parts[3]);
                        if (parts.length > 4) {
                            KdA = Float.parseFloat(parts[4]);
                        } else {
                            KdA = 1.0f;
                        }
                        break;
                    case "Ks":
                        break;
                    case "map_Kd":
                        map_Kd = mtl.getRelative(parts[1]);
                        break;
                    case "map_Bump":
                        map_Bump = mtl.getRelative(parts[1]);
                        break;
                    case "map_Ns":
                        map_Ns = mtl.getRelative(parts[1]);
                        break;
                    case "Ns":
                    case "Ke":
                    case "Ni":
                    case "d":
                    case "illum":
                    default:
                        break;
                }
            }
            if (materialName != null) {
                mat.put(materialName, new Material(materialName, map_Kd, map_Bump, map_Ns, KdR, KdG, KdB, KdA));
            }
            return mat;
        }
    }

    private void addFace(String a, String b, String c, String material) {
        parsePoint(a);
        parsePoint(b);
        parsePoint(c);
        faceMaterials.add(material);
    }

    private void addGroup(String name) {
        if (currentGroupStart != faceMaterials.size()) {
            groups.add(new ModelGroup(currentGroupName, currentGroupStart, faceMaterials.size() - 1, null, null, null));
        }
        currentGroupName = name;
        currentGroupStart = faceMaterials.size();
    }

    private void parsePoint(String point) {
        String[] sp = point.split("/");
        for (int i = 0; i < 3; i++) {
            if (i < sp.length && !sp[i].isEmpty()) {
                indices.add(Integer.parseInt(sp[i]) - 1);
            } else {
                indices.add(-1);
                if (i == 2) {
                    //VN
                    this.hasNormals = false;
                }
            }
        }
    }
}
