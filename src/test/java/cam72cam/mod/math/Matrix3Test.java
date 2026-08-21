package cam72cam.mod.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Matrix3Test {
    @Test
    public void eulerTest() {
        double yaw = 90, pitch = 3, roll = 50;
        Matrix3 mat = Matrix3.fromEuler(yaw, pitch, roll);
        Vec3d ypr = mat.toEuler();
        // yaw is normalized to (-180, 180]
        Assertions.assertEquals(yaw, ypr.x, 1e-9);
        // pitch is normalized to [-90, 90]
        Assertions.assertEquals(pitch, ypr.y, 1e-9);
        // roll is normalized to (-180, 180]
        Assertions.assertEquals(roll, ypr.z, 1e-9);
    }
}
