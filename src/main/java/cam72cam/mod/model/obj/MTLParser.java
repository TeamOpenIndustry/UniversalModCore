package cam72cam.mod.model.obj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MTLParser {
    public static List<Material> parse(InputStream stream) throws IOException {
        List<Material> materials = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            String materialName = null;
            String map_Kd = null;
            String map_Bump = null;
            String map_Ns = null;
            Float KdR = null;
            Float KdG = null;
            Float KdB = null;
            Float KdA = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.length() == 0) {
                    continue;
                }
                String[] parts = line.split(" ");
                switch (parts[0]) {
                    case "newmtl":
                        if (materialName != null) {
                            materials.add(new Material(materialName, map_Kd, map_Bump, map_Ns, KdR, KdG, KdB, KdA));
                        }
                        materialName = parts[1];
                        for (int i = 2; i < parts.length; i++) {
                            materialName += " " + parts[i];
                        }
                        map_Kd = null;
                        KdR = null;
                        KdG = null;
                        KdB = null;
                        KdA = null;
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
                        map_Kd = parts[1];
                        break;
                    case "map_Bump":
                        map_Bump = parts[1];
                        break;
                    case "map_Ns":
                        map_Ns = parts[1];
                        break;
                    case "Ns":
                        //Ignore
                        break;
                    case "Ke":
                        //Ignore
                        break;
                    case "Ni":
                        //Ignore
                        break;
                    case "d":
                        //ignore
                        break;
                    case "illum":
                        //ignore
                        break;
                    default:
                        //System.out.println("MTL: ignored line '" + line + "'");
                        break;
                }
            }
            if (materialName != null) {
                materials.add(new Material(materialName, map_Kd, map_Bump, map_Ns, KdR, KdG, KdB, KdA));
            }
            return materials;
        }
    }
}