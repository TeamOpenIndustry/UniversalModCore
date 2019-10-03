package cam72cam.mod.fluid;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.SimpleFixedFluidInv;
import cam72cam.mod.util.TagCompound;

public class FluidTank implements ITank {
    public FluidInv internal;

    private class FluidInv extends SimpleFixedFluidInv {
        FluidInv(FluidStack fluidStack, int capacity) {
            super(1, capacity);
            if (fluidStack != null) {
                this.forceSetInvFluid(0, fluidStack.internal);
            }
        }
    }

    public FluidTank(FluidStack fluidStack, int capacity) {
        internal = new FluidInv(fluidStack, capacity);
        internal.addListener((inv, tank, previous, current) -> { FluidTank.this.onChanged(); }, () -> {});
    }

    public void onChanged() {
        //NOP
    }

    @Override
    public FluidStack getContents() {
        return new FluidStack(internal.getInvFluid(0));
    }

    @Override
    public int getCapacity() {
        return internal.tankCapacity;
    }

    public void setCapacity(int milliBuckets) {
        internal = new FluidInv(getContents(), milliBuckets);
        internal.addListener((inv, tank, previous, current) -> { FluidTank.this.onChanged(); }, () -> {});
    }

    @Override
    public boolean allows(Fluid fluid) {
        return internal.isFluidValidForTank(0, fluid.internal);
    }

    @Override
    public int fill(FluidStack fluidStack, boolean simulate) {
        if (!allows(fluidStack.getFluid())) {
            return 0;
        }
        return internal.attemptInsertion(fluidStack.internal, simulate ? Simulation.SIMULATE : Simulation.ACTION).getAmount();
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, boolean simulate) {
        if (!allows(fluidStack.getFluid())) {
            return null;
        }
        return new FluidStack(internal.attemptExtraction(new ExactFluidFilter(fluidStack.internal.getFluidKey()), fluidStack.internal.getAmount(), simulate ? Simulation.SIMULATE : Simulation.ACTION));
    }

    public TagCompound write(TagCompound tag) {
        return new TagCompound(internal.toTag(tag.internal));
    }

    public void read(TagCompound tag) {
        internal.fromTag(tag.internal);
    }

    public boolean tryDrain(ITank inputTank, int max, boolean simulate) {
        //TODO broken max
        int maxTransfer = this.fill(inputTank.getContents(), true);
        maxTransfer = Math.min(maxTransfer, max);

        if (maxTransfer == 0) {
            // Out of room or limit too small
            return false;
        }

        FluidStack attemptedDrain = inputTank.drain(new FluidStack(inputTank.getContents().getFluid(), maxTransfer), true);

        if (attemptedDrain == null || attemptedDrain.getAmount() != maxTransfer) {
            // Can't transfer the full amount
            return false;
        }

        // Either attempt or do fill
        boolean ok = this.fill(inputTank.getContents(), simulate) == attemptedDrain.getAmount();

        if (!simulate) {
            // Drain input tank
            inputTank.drain(new FluidStack(inputTank.getContents().getFluid(), maxTransfer), false);
        }
        return ok;
    }

    public boolean tryFill(ITank inputTank, int max, boolean simulate) {
        //TODO broken max
        int maxTransfer = inputTank.fill(this.getContents(), true);
        maxTransfer = Math.min(maxTransfer, max);

        if (maxTransfer == 0) {
            // Out of room or limit too small
            return false;
        }

        FluidStack attemptedDrain = this.drain(new FluidStack(this.getContents().getFluid(), maxTransfer), true);

        if (attemptedDrain == null || attemptedDrain.getAmount() != maxTransfer) {
            // Can't transfer the full amount
            return false;
        }

        // Either attempt or do fill
        boolean ok = inputTank.fill(this.getContents(), simulate) == attemptedDrain.getAmount();

        if (!simulate) {
            // Drain input tank
            this.drain(new FluidStack(this.getContents().getFluid(), maxTransfer), false);
        }
        return ok;
    }
}
