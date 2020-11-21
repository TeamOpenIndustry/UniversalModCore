package cam72cam.mod.fluid;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.*;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.misc.Ref;

import java.util.Set;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.ModCore;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface ITank {
    /**
     * Get a Tank that represents the contents of an itemstack.  Assumes each item can only have a single tank.
     *
     * onUpdate is used for receiving the resultant modified itemstack without actually modifying the input stack (simulate)
     *
     * See ImmersiveRailroading's FreightTank for an example.
     */
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
                return fluid.internal.stream().anyMatch(inv.getInsertionFilter()::matches);
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

    /** Wrap Forge's IFluidHandler, do not use directly */
    static List<ITank> getTank(FixedFluidInv internal) {
        if (internal == null) {
            return null;
        }

        return IntStream.range(0, internal.getTankCount()).mapToObj(i -> new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(internal.getTank(i).get());
            }

            @Override
            public int getCapacity() {
                return internal.getTank(i).getMaxAmount();
            }

            @Override
            public boolean allows(Fluid fluid) {
                return fluid.internal.stream().anyMatch(internal.getTank(i)::isValid);
            }

            @Override
            public int fill(FluidStack fluidStack, boolean simulate) {
                return fluidStack.getAmount() - internal.getTank(i).attemptInsertion(fluidStack.internal, simulate ? Simulation.SIMULATE : Simulation.ACTION).getAmount();
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                return new FluidStack(internal.getTank(i).attemptExtraction(new ExactFluidFilter(fluidStack.internal.getFluidKey()), fluidStack.internal.getAmount(), simulate ? Simulation.SIMULATE : Simulation.ACTION));
            }
        }).collect(Collectors.toList());
    }

    /** Copy of the current contents of the tank */
    FluidStack getContents();

    /** Max capacity */
    int getCapacity();

    /** If a fluid will be accepted into this tank */
    boolean allows(Fluid fluid);

    /**
     * Attempt to fill the tank with as much of the stack as possible
     *
     * @return Amount actually transferred
     */
    int fill(FluidStack fluidStack, boolean simulate);

    /**
     * Attempt to drain the tank, whitelisting the type of the stack and up to the amount in the stack
     * @return Fluid/amount actually transferred
     */
    FluidStack drain(FluidStack fluidStack, boolean simulate);

    /**
     * Attempt to drain the inputTank into this tank
     * @return if anything was transferred
     */
    default int drain(ITank inputTank, int max, boolean simulate) {
        // Calculate max transfer into this tank
        int maxTransfer = this.fill(inputTank.getContents(), true);
        maxTransfer = Math.min(maxTransfer, max);

        if (maxTransfer == 0) {
            // Out of room or empty
            return 0;
        }

        // See if the other tank can hold this amount
        FluidStack allowedTransfer = inputTank.drain(new FluidStack(inputTank.getContents().getFluid(), maxTransfer), true);

        if (allowedTransfer == null || allowedTransfer.getAmount() == 0) {
            // Can't transfer anything
            return 0;
        }

        // Either attempt or do fill
        int transferred = this.fill(allowedTransfer, simulate);

        FluidStack check = inputTank.drain(new FluidStack(inputTank.getContents().getFluid(), maxTransfer), simulate);
        if (check.getAmount() != transferred) {
            try {
                throw new Exception("Invalid fluid transfer!");
            } catch (Exception e) {
                ModCore.catching(e);
            }
        }

        return transferred;
    }

    /**
     * Attempt to drain this tank into the inputTank
     * @return if anything was transferred
     */
    default int fill(ITank inputTank, int max, boolean simulate) {
        return inputTank.drain(this, max, simulate);
    }
}
