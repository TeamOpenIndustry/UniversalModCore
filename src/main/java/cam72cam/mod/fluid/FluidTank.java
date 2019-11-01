package cam72cam.mod.fluid;

import cam72cam.mod.util.TagCompound;

public class FluidTank implements ITank {
    private FluidStack internal;
    private int capacity;

    public FluidTank(FluidStack fluidStack, int capacity) {
        this.internal = fluidStack == null ? new FluidStack(null) : fluidStack;
        this.capacity = capacity;
    }

    public void onChanged() {
        //NOP
    }

    @Override
    public FluidStack getContents() {
        return internal;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int milliBuckets) {
        capacity = milliBuckets;
    }

    @Override
    public boolean allows(Fluid fluid) {
        return true; // TODO 1.7.10
    }

    @Override
    public int fill(FluidStack fluidStack, boolean simulate) {
        if (!allows(fluidStack.getFluid())) {
            return 0;
        }

        if (fluidStack.getAmount() < 0 || internal.getAmount() == capacity) {
            return 0;
        }

        if (internal.getAmount() == 0 || internal.getFluid().equals(fluidStack.getFluid())) {
            int amount = Math.min(fluidStack.getAmount() + internal.getAmount(), capacity);
            int prevAmount = internal.getAmount();
            if (!simulate) {
                internal = new FluidStack(fluidStack.getFluid(), amount);
                onChanged();
            }
            return amount - prevAmount;
        }
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, boolean simulate) {
        if (!allows(fluidStack.getFluid()) || internal.getAmount() == 0) {
            return null;
        }

        if (internal.getFluid().equals(fluidStack.getFluid())) {
            int remainder = Math.min(internal.getAmount() - fluidStack.getAmount(), 0);
            if (!simulate) {
                internal = new FluidStack(fluidStack.getFluid(), remainder);
                onChanged();
            }
            return new FluidStack(fluidStack.getFluid(), fluidStack.getAmount() - remainder);
        }
        return null;
    }

    public TagCompound write(TagCompound tag) {
        TagCompound tc = new TagCompound();
        tc.setInteger("capacity", capacity);
        if (internal.getAmount() != 0) {
            internal.internal.writeToNBT(tc.internal);
        }
        return tc;
    }

    public void read(TagCompound tag) {
        capacity = tag.getInteger("capacity");
        internal = new FluidStack(net.minecraftforge.fluids.FluidStack.loadFluidStackFromNBT(tag.internal));
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
