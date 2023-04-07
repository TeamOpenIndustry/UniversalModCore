package cam72cam.mod.fluid;

import cam72cam.mod.serialization.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@TagMapped(FluidTank.Mapper.class)
public class FluidTank implements ITank {
    // TODO clean up capacity crap.  Probably just want to implement my own fluid handler from scratch TBH

    public final net.minecraftforge.fluids.FluidTank internal;
    private Supplier<List<Fluid>> filter;
    private final Set<Runnable> onChange = new HashSet<>();

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
                        onChange();
                    }
                }
                public int fill(net.minecraftforge.fluids.FluidStack resource, boolean doFill) {
                    int res = super.fill(resource, doFill);
                    if (doFill && res != 0) {
                        onChange();
                    }
                    return res;
                }
                public net.minecraftforge.fluids.FluidStack drain(int maxDrain, boolean doDrain) {
                    net.minecraftforge.fluids.FluidStack res = super.drain(maxDrain, doDrain);
                    if (res != null && doDrain) {
                        onChange();
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
                        onChange();
                    }
                }
                public int fill(net.minecraftforge.fluids.FluidStack resource, boolean doFill) {
                    int res = super.fill(resource, doFill);
                    if (doFill && res != 0) {
                        onChange();
                    }
                    return res;
                }
                public net.minecraftforge.fluids.FluidStack drain(int maxDrain, boolean doDrain) {
                    net.minecraftforge.fluids.FluidStack res = super.drain(maxDrain, doDrain);
                    if (res != null && doDrain) {
                        onChange();
                    }
                    return res;
                }
            };
        }
    }

    private void onChange() {
        onChange.forEach(Runnable::run);
    }

    /** Add onChanged handler */
    public void onChanged(Runnable onChange) {
        this.onChange.add(onChange);
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
            return new FluidStack(null, 0);
        }
        return new FluidStack(internal.drain(fluidStack.internal.amount, !simulate));
    }

    public TagCompound write(TagCompound tag) {
        return new TagCompound(internal.writeToNBT(tag.internal));
    }

    public void read(TagCompound tag) {
        internal.readFromNBT(tag.internal);
    }

    static class Mapper implements TagMapper<FluidTank> {
        @Override
        public TagAccessor<FluidTank> apply(Class<FluidTank> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                    ((d, o) -> {
                        if (o == null) {
                            d.remove(fieldName);
                            return;
                        }
                        d.set(fieldName, o.write(new TagCompound()));
                    }),
                    d -> {
                        FluidTank ft = new FluidTank(null, 0);
                        ft.read(d.get(fieldName));
                        return ft;
                    }
            );
        }
    }
}
