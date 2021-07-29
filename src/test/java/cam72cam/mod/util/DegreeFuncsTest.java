package cam72cam.mod.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DegreeFuncsTest {

    @org.junit.jupiter.api.Test
    public void testNormalize() {
        float[] cases = new float[]{
                0, 0,
                180, 180,
                -180, 180,
                360, 0,
                -360, 0,
                400, 40,
                -400, 320,
                360 * 10 + 30, 30,
                -360 * 10 -30, 330,
        };
        for (int i = 0; i < cases.length; i+=2) {
            assertEquals(cases[i+1], DegreeFuncs.normalize(cases[i]));
        }
    }

    @org.junit.jupiter.api.Test
    public void testDelta() {
        float[] cases = new float[]{
                0, 0, 0,
                180, 180, 0,
                -180, 180, 0,
                360, 0, 0,
                -360, 0, 0,
                400, 40, 0,
                -400, 320, 0,
                360 * 10 + 30, 30, 0,
                -360 * 10 -30, 330, 0,
                10, 20, 10,
                0, 179, 179,
                0, 180, 180,
                0, 181, 179,
                -90, 90, 180,
        };
        for (int i = 0; i < cases.length; i+=3) {
            assertEquals(cases[i+2], DegreeFuncs.delta(cases[i], cases[i+1]));
        }
    }
}