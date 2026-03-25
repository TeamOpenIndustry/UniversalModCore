//AI Generated
package cam72cam.mod.render;

import net.minecraft.util.math.MathHelper;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A class used for smoothing input values with Hermite function
 */
public class SmoothFloat {
    private static final HashSet<WeakReference<SmoothFloat>> instances = new HashSet<>();

    private float currentValue;
    private float currentVelocity;

    private float startValue;
    private float startVelocity;

    private float targetValue;
    private float targetVelocity;

    private float durationTicks;
    private float elapsedTicks;
    private boolean active;

    public SmoothFloat() {
        this(0.0f);
    }

    public SmoothFloat(float value) {
        this.currentValue = value;
        this.currentVelocity = 0.0f;

        this.startValue = value;
        this.startVelocity = 0.0f;

        this.targetValue = value;
        this.targetVelocity = 0.0f;

        this.durationTicks = 0.0f;
        this.elapsedTicks = 0.0f;
        this.active = false;

        instances.add(new WeakReference<>(this));
    }

    public float getValue(float partialTicks) {
        if (!active || durationTicks <= 0.0f) {
            return currentValue;
        }

        float t = MathHelper.clamp((elapsedTicks + MathHelper.clamp(partialTicks, 0, 1)) / durationTicks, 0, 1);
        return evaluatePosition(t);
    }

    public void setNewValue(float newValue, float expectedTicks) {
        if (expectedTicks <= 0.0f) {
            currentValue = newValue;
            currentVelocity = 0.0f;

            startValue = newValue;
            startVelocity = 0.0f;

            targetValue = newValue;
            targetVelocity = 0.0f;

            durationTicks = 0.0f;
            elapsedTicks = 0.0f;
            active = false;
            return;
        }


        float v = getCurrentVelocityAtTime(elapsedTicks, durationTicks);

        currentValue = getValue(0.0f);

        startValue = currentValue;
        startVelocity = v;

        targetValue = newValue;
        targetVelocity = 0.0f;

        durationTicks = expectedTicks;
        elapsedTicks = 0.0f;
        active = true;
    }

    private void tick() {
        if (!active || durationTicks <= 0.0f) {
            return;
        }

        elapsedTicks += 1.0f;

        if (elapsedTicks >= durationTicks) {
            currentValue = targetValue;
            currentVelocity = targetVelocity;
            active = false;
            return;
        }

        float t = MathHelper.clamp(elapsedTicks / durationTicks, 0, 1);
        currentValue = evaluatePosition(t);
        currentVelocity = evaluateVelocity(t) / durationTicks;
    }


    public static void onClientTick() {
        Iterator<WeakReference<SmoothFloat>> it = instances.iterator();
        while (it.hasNext()) {
            WeakReference<SmoothFloat> ref = it.next();
            SmoothFloat instance = ref.get();
            if (instance != null) {
                instance.tick();
            } else {
                it.remove();
            }
        }
    }

    private float evaluatePosition(float t) {
        t = MathHelper.clamp(t, 0, 1);

        float t2 = t * t;
        float t3 = t2 * t;

        float h00 = 2.0f * t3 - 3.0f * t2 + 1.0f;
        float h10 = t3 - 2.0f * t2 + t;
        float h01 = -2.0f * t3 + 3.0f * t2;
        float h11 = t3 - t2;

        return h00 * startValue
                + h10 * startVelocity * durationTicks
                + h01 * targetValue
                + h11 * targetVelocity * durationTicks;
    }

    private float evaluateVelocity(float t) {
        t = MathHelper.clamp(t, 0, 1);

        float t2 = t * t;

        float dh00 = 6.0f * t2 - 6.0f * t;
        float dh10 = 3.0f * t2 - 4.0f * t + 1.0f;
        float dh01 = -6.0f * t2 + 6.0f * t;
        float dh11 = 3.0f * t2 - 2.0f * t;

        return dh00 * startValue
                + dh10 * startVelocity * durationTicks
                + dh01 * targetValue
                + dh11 * targetVelocity * durationTicks;
    }


    private float getCurrentVelocityAtTime(float elapsedTicks, float durationTicks) {
        if (!active || durationTicks <= 0.0f) {
            return currentVelocity;
        }

        float t = MathHelper.clamp(elapsedTicks / durationTicks, 0, 1);
        return evaluateVelocity(t) / durationTicks;
    }
}