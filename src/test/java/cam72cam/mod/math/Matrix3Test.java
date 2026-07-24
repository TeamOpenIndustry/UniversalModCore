package cam72cam.mod.math;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class Matrix3Test {
    @Test
    public void eulerTest() {
        double yaw = -5, pitch = 3, roll = -10;
        Matrix3 mat = Matrix3.fromEuler(yaw, pitch, roll);
        System.out.println(mat);
        Vec3d ypr = mat.toEuler();
        System.out.println(ypr);
    }

    @Test
    public void vecCache() {
        // If we compare and return then there's about 4x slower than rebuilding after every write
        // Cache like Entity.java -> 8500000 ns
        // Make them private and calculate once -> 2500000ns
        // For 400000 runs
        List<Vec3d> arrayList = new ArrayList<>(400000);
        Matrix3 mat = new Matrix3();
        // Warmup JITs
        for (int j = 0; j < 400000; j++) {
            arrayList.add(mat.forward());
        }
        mat = new Matrix3();
        arrayList.clear();
        long start = System.nanoTime();
        for (int j = 0; j < 400000; j++) {
            arrayList.add(mat.forward());
        }
        long end = System.nanoTime();
        System.out.println(end - start);
        System.out.println(arrayList.size());
    }
}
