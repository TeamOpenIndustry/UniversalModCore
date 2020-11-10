package cam72cam.mod.fluid;

import cam72cam.mod.ModCore;
import cam72cam.mod.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

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
        LazyOptional<IFluidHandlerItem> handler = FluidUtil.getFluidHandler(inputCopy.internal);
        if (!handler.isPresent()) {
            return null;
        }
        IFluidHandlerItem internal = handler.orElse(null);
        return new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(internal.getFluidInTank(0));
            }

            @Override
            public int getCapacity() {
                return internal.getTankCapacity(0);
            }

            @Override
            public boolean allows(Fluid fluid) {
                return internal.isFluidValid(0, new net.minecraftforge.fluids.FluidStack(fluid.internal, 1));
            }

            @Override
            public int fill(FluidStack fluidStack, boolean simulate) {
                IFluidHandlerItem temp = FluidUtil.getFluidHandler(inputCopy.copy().internal).orElse(null);
                temp.fill(fluidStack.internal, IFluidHandler.FluidAction.EXECUTE);
                onUpdate.accept(new ItemStack(temp.getContainer()));

                return internal.fill(fluidStack.internal, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                IFluidHandlerItem temp = FluidUtil.getFluidHandler(inputCopy.copy().internal).orElse(null);
                temp.drain(fluidStack.internal, IFluidHandler.FluidAction.EXECUTE);
                onUpdate.accept(new ItemStack(temp.getContainer()));

                return new FluidStack(internal.drain(fluidStack.internal, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE));
            }
        };
    }

    /** Wrap Forge's IFluidHandler, do not use directly */
    static List<ITank> getTank(IFluidHandler internal) {
        return IntStream.range(0, internal.getTanks()).mapToObj(i -> new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(internal.getFluidInTank(i));
            }

            @Override
            public int getCapacity() {
                return internal.getTankCapacity(i);
            }

            @Override
            public boolean allows(Fluid fluid) {
                return internal.isFluidValid(i, new net.minecraftforge.fluids.FluidStack(fluid.internal, 1));
            }

            @Override
            public int fill(FluidStack fluidStack, boolean simulate) {
                // BUG: This is a pretty fundamental problem with how forge's fluid API works.
                // IFluidHandler should really expose a list of distinct tanks
                return internal.fill(fluidStack.internal, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                // BUG: This is a pretty fundamental problem with how forge's fluid API works.
                // IFluidHandler should really expose a list of distinct tanks
                return new FluidStack(internal.drain(fluidStack.internal, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE));
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
