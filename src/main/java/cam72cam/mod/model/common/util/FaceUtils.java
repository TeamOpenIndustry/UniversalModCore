package cam72cam.mod.model.common.util;

import cam72cam.mod.math.Vec3d;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.List;
import java.util.stream.IntStream;

public class FaceUtils {
    /**
     * Triangulate given vertices
     * @param vertices Ordered vertices
     * @return Sorted indices, 3 per face
     */
    public static int[] triangulate(List<Vec3d> vertices) {
        int n = vertices.size();
        if (n < 3) {
            return new int[0];
        } else if (n == 3) {
            return new int[]{0, 1, 2};
        }

        // Newell normal, follows the vertex winding so the dot-product side tests stay consistent
        Vec3d normal = Vec3d.ZERO;
        for (int i = 0; i < n; i++) {
            Vec3d a = vertices.get(i);
            Vec3d b = vertices.get((i + 1) % n);
            normal = normal.add(new Vec3d((a.y - b.y) * (a.z + b.z), (a.z - b.z) * (a.x + b.x), (a.x - b.x) * (a.y + b.y)));
        }

        Buffers.IntBuffer result = new Buffers.IntBuffer(n * 2);

        List<Integer> remaining = new IntArrayList(IntStream.range(0, n).toArray());
        while (remaining.size() > 3) {
            int ear = -1;
            for (int i = 0; i < remaining.size(); i++) {
                int prev = remaining.get((i + remaining.size() - 1) % remaining.size());
                int curr = remaining.get(i);
                int next = remaining.get((i + 1) % remaining.size());

                Vec3d a = vertices.get(prev);
                Vec3d b = vertices.get(curr);
                Vec3d c = vertices.get(next);

                // Convex corner: a left turn for the polygon's winding
                if (b.subtract(a).crossProduct(c.subtract(b)).dotProduct(normal) <= 0) {
                    continue;
                }

                // Valid ear: no other remaining vertex lies inside triangle (a, b, c)
                boolean hasPointInside = false;
                for (int k : remaining) {
                    if (k == prev || k == curr || k == next) {
                        continue;
                    }
                    Vec3d p = vertices.get(k);
                    double d1 = b.subtract(a).crossProduct(p.subtract(a)).dotProduct(normal);
                    double d2 = c.subtract(b).crossProduct(p.subtract(b)).dotProduct(normal);
                    double d3 = a.subtract(c).crossProduct(p.subtract(c)).dotProduct(normal);
                    if ((d1 < 0 || d2 < 0 || d3 < 0) && (d1 > 0 || d2 > 0 || d3 > 0)) {
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
