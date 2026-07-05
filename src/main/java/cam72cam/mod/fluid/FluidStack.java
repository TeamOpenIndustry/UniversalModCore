package cam72cam.mod.fluid;

/** Wrapper around forge FluidStack */
public class FluidStack {
    public final net.minecraftforge.fluids.FluidStack internal;

    /** Wrapper, don't use directly */
    public FluidStack(net.minecraftforge.fluids.FluidStack stack) {
        this.internal = stack;
    }

    public FluidStack(Fluid fluid, int amount) {
        this.internal = fluid != null && fluid.internal != null ? new net.minecraftforge.fluids.FluidStack(fluid.internal.get(0), amount) : null;
    }

    public Fluid getFluid() {
        if (internal == null) {
            return null;
        }
        return Fluid.getFluid(internal.getFluid());
    }

    public int getAmount() {
        if (internal == null) {
            return 0;
        }
        return internal.getAmount();
    }
}
