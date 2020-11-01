package cam72cam.mod.util;

public class DegreeFuncs {
    /** range 0 to 360 */
    public static float normalize(float value) {
        return (((value % 360) + 360)  % 360);
    }

    public static float delta(float a, float b) {
        float delta = normalize(a - b);
        return Math.min(360 - delta, delta);
    }

}
