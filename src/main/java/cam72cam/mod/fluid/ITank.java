package cam72cam.mod.fluid;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.*;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.misc.Ref;
import cam72cam.mod.item.ItemStack;

import java.util.Set;
import java.util.function.Consumer;

public interface ITank {
    static ITank getTank(ItemStack inputCopy, Consumer<ItemStack> onUpdate) {
        GroupedFluidInv inv = FluidAttributes.GROUPED_INV.getFirstOrNull(new Ref<>(inputCopy.internal));
        if (inv == null) {
            return null;
        }

        return new ITank() {
            @Override
            public FluidStack getContents() {
                Set<FluidKey> fluids = inv.getStoredFluids();
                if (fluids.size() == 0) {
                    return new FluidStack(null);
                }
                FluidKey fluid = (FluidKey) fluids.toArray()[0];
                return new FluidStack(Fluid.getFluid(fluid), inv.getAmount(fluid));
            }

            @Override
            public int getCapacity() {
                return inv.getTotalCapacity();
            }

            @Override
            public boolean allows(Fluid fluid) {
                return inv.getInsertionFilter().matches(fluid.internal);
            }

            @Override
            public int fill(FluidStack fluidStack, boolean simulate) {
                ItemStack ts = inputCopy.copy();
                Ref<net.minecraft.item.ItemStack> ref = new Ref<>(ts.internal);
                GroupedFluidInv temp = FluidAttributes.GROUPED_INV.get(ref);
                temp.attemptInsertion(fluidStack.internal, Simulation.ACTION);
                onUpdate.accept(new ItemStack(ref.get()));

                return fluidStack.getAmount() - inv.attemptInsertion(fluidStack.internal, simulate ? Simulation.SIMULATE : Simulation.ACTION).getAmount();
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                ItemStack ts = inputCopy.copy();
                Ref<net.minecraft.item.ItemStack> ref = new Ref<>(ts.internal);
                GroupedFluidInv temp = FluidAttributes.GROUPED_INV.get(ref);
                temp.attemptExtraction(new ExactFluidFilter(fluidStack.internal.getFluidKey()), fluidStack.internal.getAmount(), Simulation.ACTION);
                onUpdate.accept(new ItemStack(ref.get()));

                return new FluidStack(inv.attemptExtraction(new ExactFluidFilter(fluidStack.internal.getFluidKey()), fluidStack.internal.getAmount(), simulate ? Simulation.SIMULATE : Simulation.ACTION));
            }
        };
    }

    static ITank getTank(FixedFluidInv internal) {
        if (internal == null) {
            return null;
        }

        return new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(internal.getTank(0).get());
            }

            @Override
            public int getCapacity() {
                return internal.getTank(0).getMaxAmount();
            }

            @Override
            public boolean allows(Fluid fluid) {
                return internal.getTank(0).isValid(fluid.internal);
            }

            @Override
            public int fill(FluidStack fluidStack, boolean simulate) {
                return fluidStack.getAmount() - internal.getTank(0).attemptInsertion(fluidStack.internal, simulate ? Simulation.SIMULATE : Simulation.ACTION).getAmount();
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                return new FluidStack(internal.getTank(0).attemptExtraction(new ExactFluidFilter(fluidStack.internal.getFluidKey()), fluidStack.internal.getAmount(), simulate ? Simulation.SIMULATE : Simulation.ACTION));
            }
        };
    }

    FluidStack getContents();

    int getCapacity();

    boolean allows(Fluid fluid);

    int fill(FluidStack fluidStack, boolean simulate);

    FluidStack drain(FluidStack fluidStack, boolean simulate);

}
