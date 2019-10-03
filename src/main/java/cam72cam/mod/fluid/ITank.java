package cam72cam.mod.fluid;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.*;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.misc.Ref;
import cam72cam.mod.item.ItemStack;

import java.util.function.Consumer;

public interface ITank {
    static ITank getTank(ItemStack inputCopy, Consumer<ItemStack> onUpdate) {
        if (!inputCopy.isFluidContainer()) {
            return null;
        }

        FixedFluidInv inv = FluidAttributes.FIXED_INV.get(new Ref<>(inputCopy.internal));

        return new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(inv.getTank(0).get());
            }

            @Override
            public int getCapacity() {
                return inv.getTank(0).getMaxAmount();
            }

            @Override
            public boolean allows(Fluid fluid) {
                return inv.getTank(0).isValid(fluid.internal);
            }

            @Override
            public int fill(FluidStack fluidStack, boolean simulate) {
                ItemStack ts = inputCopy.copy();
                FixedFluidInv temp = FluidAttributes.FIXED_INV.get(new Ref<>(ts.internal));
                temp.getTank(0).attemptInsertion(fluidStack.internal, Simulation.ACTION);
                onUpdate.accept(ts);

                return inv.getTank(0).attemptInsertion(fluidStack.internal, simulate ? Simulation.SIMULATE : Simulation.ACTION).getAmount();
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                ItemStack ts = inputCopy.copy();
                FixedFluidInv temp = FluidAttributes.FIXED_INV.get(new Ref<>(ts.internal));
                temp.getTank(0).attemptExtraction(new ExactFluidFilter(fluidStack.internal.getFluidKey()), fluidStack.internal.getAmount(), Simulation.ACTION);
                onUpdate.accept(ts);

                return new FluidStack(inv.getTank(0).attemptExtraction(new ExactFluidFilter(fluidStack.internal.getFluidKey()), fluidStack.internal.getAmount(), simulate ? Simulation.SIMULATE : Simulation.ACTION));
            }
        };
    }

    static ITank getTank(FixedFluidInv internal) {
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
                return internal.getTank(0).attemptInsertion(fluidStack.internal, simulate ? Simulation.SIMULATE : Simulation.ACTION).getAmount();
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
