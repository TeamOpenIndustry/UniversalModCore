package cam72cam.mod.fluid;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class FluidStack {
    public final FluidVolume internal;

    public FluidStack(FluidVolume stack) {
        this.internal = stack;
    }

    public FluidStack(Fluid fluid, int amount) {
        this.internal = fluid != null && fluid.internal != null ? fluid.internal.withAmount(amount) : null;
    }

    public Fluid getFluid() {
        if (internal == null) {
            return null;
        }
        return Fluid.getFluid(internal.fluidKey);
    }

    public int getAmount() {
        if (internal == null) {
            return 0;
        }
        return internal.getAmount();
    }
}
