package cam72cam.mod.model.common.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

public class FaceUtils {

    @Unmodifiable
    public static final int[] TRIANGLE = new int[]{0, 1, 2};

    /**
     * Triangulates the given polygon vertices using the ear-cut method.
     *
     * @param vertices Ordered polygon vertices, 3 per face
     * @return Triangulated indices, 3 per face
     */
    public static int[] triangulate(float[] vertices) {
        int n = vertices.length / 3;
        if (n < 3) {
            return new int[0];
        } else if (n == 3) {
            return TRIANGLE;
        }

        // Newell normal, follows the vertex winding so the dot-product side tests stay consistent
        float nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < n; i++) {
            int aIdx = i * 3;
            int bIdx = ((i + 1) % n) * 3;
            float ax = vertices[aIdx], ay = vertices[aIdx + 1], az = vertices[aIdx + 2];
            float bx = vertices[bIdx], by = vertices[bIdx + 1], bz = vertices[bIdx + 2];
            //normal.add(new Vec3d((a.y - b.y) * (a.z + b.z), (a.z - b.z) * (a.x + b.x), (a.x - b.x) * (a.y + b.y)))
            nx += (ay - by) * (az + bz); ny += (az - bz) * (ax + bx); nz += (ax - bx) * (ay + by);
        }

        Buffers.IntBuffer result = new Buffers.IntBuffer(n * 2);

        List<Integer> remaining = new IntArrayList(n);
        for (int i = 0; i < n; i++) {
            remaining.add(i);
        }
        while (remaining.size() > 3) {
            int ear = -1;
            for (int i = 0; i < remaining.size(); i++) {
                int prev = remaining.get((i + remaining.size() - 1) % remaining.size());
                int curr = remaining.get(i);
                int next = remaining.get((i + 1) % remaining.size());

                float ax = vertices[prev * 3], ay = vertices[prev * 3 + 1], az = vertices[prev * 3 + 2];
                float bx = vertices[curr * 3], by = vertices[curr * 3 + 1], bz = vertices[curr * 3 + 2];
                float cx = vertices[next * 3], cy = vertices[next * 3 + 1], cz = vertices[next * 3 + 2];

                float abX = bx - ax, abY = by - ay, abZ = bz - az;
                float bcX = cx - bx, bcY = cy - by, bcZ = cz - bz;
                float acX = ax - cx, acY = ay - cy, acZ = az - cz;


                // Convex corner: a left turn for the polygon's winding
                // b.subtract(a).crossProduct(c.subtract(b)).dotProduct(normal)
                if ((abY * bcZ - abZ * bcY) * nx + (abZ * bcX - abX * bcZ) * ny + (abX * bcY - abY * bcX) * nz <= 0) {
                    continue;
                }

                // Valid ear: no other remaining vertex lies inside triangle (a, b, c)
                boolean hasPointInside = false;
                for (int k : remaining) {
                    if (k == prev || k == curr || k == next) {
                        continue;
                    }
                    float px = vertices[k * 3], py = vertices[k * 3 + 1], pz = vertices[k * 3 + 2];

                    float apX = px - ax, apY = py - ay, apZ = pz - az;
                    float bpX = px - bx, bpY = py - by, bpZ = pz - bz;
                    float cpX = px - cx, cpY = py - cy, cpZ = pz - cz;

                    // d1 = (b - a) cross (p - a) dot normal
                    float d1 = (abY * apZ - abZ * apY) * nx + (abZ * apX - abX * apZ) * ny + (abX * apY - abY * apX) * nz;
                    // d2 = (c - b) cross (p - b) dot normal
                    float d2 = (bcY * bpZ - bcZ * bpY) * nx + (bcZ * bpX - bcX * bpZ) * ny + (bcX * bpY - bcY * bpX) * nz;
                    // d3 = (a - c) cross (p - c) dot normal
                    float d3 = (acY * cpZ - acZ * cpY) * nx + (acZ * cpX - acX * cpZ) * ny + (acX * cpY - acY * cpX) * nz;
                    if ((d1 >= 0 && d2 >= 0 && d3 >= 0) || (d1 <= 0 && d2 <= 0 && d3 <= 0)) {
                        hasPointInside = true;
                        break;
                    }
                }

                if (!hasPointInside) {
                    ear = i;
                    break;
                }
            }

            if (ear < 0) {
                // Degenerate polygon, fall back to a fan
                for (int i = 1; i + 1 < n; i++) {
                    result.add(0);
                    result.add(i);
                    result.add(i + 1);
                }
                return result.array();
            }

            result.add(remaining.get((ear + remaining.size() - 1) % remaining.size())); //prev
            result.add(remaining.get(ear)); //current
            result.add(remaining.get((ear + 1) % remaining.size())); //next
            remaining.remove(ear);
        }

        result.add(remaining.get(0));
        result.add(remaining.get(1));
        result.add(remaining.get(2));
        return result.array();
    }
}
