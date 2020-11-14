package cam72cam.mod.fluid;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.SimpleFixedFluidInv;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@TagMapped(FluidTank.Mapper.class)
public class FluidTank implements ITank {
    public FluidInv internal;
    private Supplier<List<Fluid>> filter;
    private final Set<Runnable> onChange = new HashSet<>();

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
        internal.addListener((inv, tank, previous, current) -> { FluidTank.this.onChange(); }, () -> {});
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
        return new FluidStack(internal.getInvFluid(0));
    }

    @Override
    public int getCapacity() {
        return internal.tankCapacity;
    }

    public void setCapacity(int milliBuckets) {
        internal = new FluidInv(getContents(), milliBuckets);
        internal.addListener((inv, tank, previous, current) -> FluidTank.this.onChange(), () -> {});
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
        return (filter == null || filter.get() == null || filter.get().contains(fluid)) && internal.isFluidValidForTank(0, fluid.internal);
    }

    @Override
    public int fill(FluidStack fluidStack, boolean simulate) {
        if (!allows(fluidStack.getFluid())) {
            return 0;
        }
        return fluidStack.internal.getAmount() - internal.attemptInsertion(fluidStack.internal, simulate ? Simulation.SIMULATE : Simulation.ACTION).getAmount();
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
