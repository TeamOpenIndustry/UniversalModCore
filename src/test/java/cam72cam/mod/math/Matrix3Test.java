package cam72cam.mod.math;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class Matrix3Test {
    @Test
    public void eulerTest() {
        double yaw = 90, pitch = 3, roll = 50;
        Matrix3 mat = Matrix3.fromEuler(yaw, pitch, roll);
        Vec3d ypr = mat.toEuler();
        // yaw is normalized to (-180, 180]
        Assert.assertEquals(yaw, ypr.x, 1e-9);
        // pitch is normalized to [-90, 90]
        Assert.assertEquals(pitch, ypr.y, 1e-9);
        // roll is normalized to (-180, 180]
        Assert.assertEquals(roll, ypr.z, 1e-9);
    }
}
