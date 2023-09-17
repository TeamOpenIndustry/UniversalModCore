package cam72cam.mod.energy;

import cam72cam.mod.serialization.TagField;

import java.util.ArrayList;
import java.util.List;

/** Reference IEnergy implementation */
public class Energy implements IEnergy {
    @TagField("max")
    private int max;
    @TagField("stored")
    private int stored;

    private final List<Runnable> onChanged = new ArrayList<>();

    private Energy() {
        // Serialization
        max = 0;
        stored = 0;
    }

    public Energy(int stored, int maxStorage) {
        this.stored = stored;
        this.max = maxStorage;
    }

    /** Subscribe to on-change event */
    public void onChanged(Runnable fn) {
        onChanged.add(fn);
    }

    @Override
    public int receive(int maxReceive, boolean simulate) {
        int delta = Math.min(maxReceive, max - stored);
        if (!simulate && delta != 0) {
            this.stored += delta;
            onChanged.forEach(Runnable::run);
        }
        return delta;
    }

    @Override
    public int extract(int maxExtract, boolean simulate) {
        int delta = Math.min(maxExtract, stored);
        if (!simulate && delta != 0) {
            this.stored -= delta;
            onChanged.forEach(Runnable::run);
        }
        return delta;
    }

    @Override
    public int getCurrent() {
        return stored;
    }

    @Override
    public int getMax() {
        return max;
    }
}
