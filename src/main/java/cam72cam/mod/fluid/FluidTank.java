package cam72cam.mod.fluid;

import cam72cam.mod.serialization.TagCompound;

import java.util.List;
import java.util.function.Supplier;

public class FluidTank implements ITank {
    // TODO clean up capacity crap.  Probably just want to implement my own fluid handler from scratch TBH

    public final net.minecraftforge.fluids.FluidTank internal;
    private Supplier<List<Fluid>> filter;
    private Runnable onChange = () -> {};

    private FluidTank() {
        // Serialization
        this(null, 0);
    }

    public FluidTank(FluidStack fluidStack, int capacity) {
        if (fluidStack == null) {
            internal = new net.minecraftforge.fluids.FluidTank(capacity) {
                @Override
                public void setFluid(net.minecraftforge.fluids.FluidStack fluid) {
                    super.setFluid(fluid);
                    if (this.fluid == null || !this.fluid.isFluidStackIdentical(fluid)) {
                        FluidTank.this.onChange.run();
                    }
                }
                public int fill(net.minecraftforge.fluids.FluidStack resource, boolean doFill) {
                    int res = super.fill(resource, doFill);
                    if (doFill && res != 0) {
                        FluidTank.this.onChange.run();
                    }
                    return res;
                }
                public net.minecraftforge.fluids.FluidStack drain(int maxDrain, boolean doDrain) {
                    net.minecraftforge.fluids.FluidStack res = super.drain(maxDrain, doDrain);
                    if (res != null && doDrain) {
                        FluidTank.this.onChange.run();
                    }
                    return res;
                }
            };
        } else {
            internal = new net.minecraftforge.fluids.FluidTank(fluidStack.internal, capacity) {
                @Override
                public void setFluid(net.minecraftforge.fluids.FluidStack fluid) {
                    super.setFluid(fluid);
                    if (this.fluid == null || !this.fluid.isFluidStackIdentical(fluid)) {
                        FluidTank.this.onChange.run();
                    }
                }
                public int fill(net.minecraftforge.fluids.FluidStack resource, boolean doFill) {
                    int res = super.fill(resource, doFill);
                    if (doFill && res != 0) {
                        FluidTank.this.onChange.run();
                    }
                    return res;
                }
                public net.minecraftforge.fluids.FluidStack drain(int maxDrain, boolean doDrain) {
                    net.minecraftforge.fluids.FluidStack res = super.drain(maxDrain, doDrain);
                    if (res != null && doDrain) {
                        FluidTank.this.onChange.run();
                    }
                    return res;
                }
            };
        }
    }

    public void onChanged(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    public FluidStack getContents() {
        return new FluidStack(internal.getFluid());
    }

    @Override
    public int getCapacity() {
        return internal.getCapacity();
    }

    public void setCapacity(int milliBuckets) {
        if (internal.getFluidAmount() > milliBuckets) {
            internal.drain(internal.getFluidAmount() - milliBuckets, true);
        }
        internal.setCapacity(milliBuckets);
    }

    /**
     * null == all
     * [] == none
     */
    public void setFilter(Supplier<List<Fluid>> filter) {
        this.filter = filter;
    }

    @Override
    public boolean allows(Fluid fluid) {
        return (filter == null || filter.get() == null || filter.get().contains(fluid));
    }

    @Override
    public int fill(FluidStack fluidStack, boolean simulate) {
        if (!allows(fluidStack.getFluid())) {
            return 0;
        }
        return internal.fill(fluidStack.internal, !simulate);
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, boolean simulate) {
        if (!allows(fluidStack.getFluid())) {
            return null;
        }
        return new FluidStack(internal.drain(fluidStack.internal.amount, !simulate));
    }

    public TagCompound write(TagCompound tag) {
        return new TagCompound(internal.writeToNBT(tag.internal));
    }

    public void read(TagCompound tag) {
        internal.readFromNBT(tag.internal);
    }

    public boolean tryDrain(ITank inputTank, int max, boolean simulate) {
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
        boolean ok = this.fill(attemptedDrain, simulate) == attemptedDrain.getAmount();

        if (!simulate) {
            // Drain input tank
            inputTank.drain(new FluidStack(inputTank.getContents().getFluid(), maxTransfer), false);
        }
        return ok;
    }

    public boolean tryFill(ITank inputTank, int max, boolean simulate) {
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
        boolean ok = inputTank.fill(attemptedDrain, simulate) == attemptedDrain.getAmount();

        if (!simulate) {
            // Drain input tank
            this.drain(new FluidStack(this.getContents().getFluid(), maxTransfer), false);
        }
        return ok;
    }
}
