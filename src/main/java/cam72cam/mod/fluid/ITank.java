package cam72cam.mod.fluid;

import cam72cam.mod.ModCore;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.util.Facing;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface ITank {
    /**
     * Get a Tank that represents the contents of an itemstack.  Assumes each item can only have a single tank.
     *
     * onUpdate is used for receiving the resultant modified itemstack without actually modifying the input stack (simulate)
     *
     * See ImmersiveRailroading's FreightTank for an example.
     */
    static ITank getTank(ItemStack inputCopy, Consumer<ItemStack> onUpdate) {
        if (inputCopy.isEmpty()) {
            return null;
        }

        if (inputCopy.internal.getItem() instanceof IFluidContainerItem) {
            IFluidContainerItem internal = (IFluidContainerItem) inputCopy.internal.getItem();
            return new ITank() {
                @Override
                public FluidStack getContents() {
                    return new FluidStack(internal.getFluid(inputCopy.internal));
                }

                @Override
                public int getCapacity() {
                    return internal.getCapacity(inputCopy.internal);
                }

                @Override
                public boolean allows(Fluid fluid) {
                    return true; // TODO 1.7.10
                }

                @Override
                public int fill(FluidStack fluidStack, boolean simulate) {
                    ItemStack ic = inputCopy.copy();
                    IFluidContainerItem temp = (IFluidContainerItem) ic.internal.getItem();
                    temp.fill(ic.internal, fluidStack.internal, true);
                    onUpdate.accept(ic);

                    return internal.fill(inputCopy.internal, fluidStack.internal, !simulate);
                }

                @Override
                public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                    ItemStack ic = inputCopy.copy();
                    IFluidContainerItem temp = (IFluidContainerItem) ic.internal.getItem();
                    temp.drain(ic.internal, fluidStack.internal.amount, true);
                    onUpdate.accept(ic);

                    return new FluidStack(internal.drain(inputCopy.internal, fluidStack.internal.amount, !simulate));
                }
            };
        }
        if (FluidContainerRegistry.isContainer(inputCopy.internal)) {
            return new ITank() {
                @Override
                public FluidStack getContents() {
                    return FluidContainerRegistry.isFilledContainer(inputCopy.internal) ? new FluidStack(FluidContainerRegistry.getFluidForFilledItem(inputCopy.internal)) : new FluidStack(null);
                }

                @Override
                public int getCapacity() {
                    return FluidContainerRegistry.getContainerCapacity(inputCopy.internal);
                }

                @Override
                public boolean allows(Fluid fluid) {
                    return true;// TODO 1.7.10
                }

                @Override
                public int fill(FluidStack fluidStack, boolean simulate) {
                    if (FluidContainerRegistry.isFilledContainer(inputCopy.internal)) {
                        return 0;
                    }

                    net.minecraft.item.ItemStack output = FluidContainerRegistry.fillFluidContainer(fluidStack.internal, inputCopy.internal);
                    onUpdate.accept(new ItemStack(output));

                    return FluidContainerRegistry.isFilledContainer(output) ? FluidContainerRegistry.getFluidForFilledItem(output).amount : 0;
                }

                @Override
                public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                    if (!FluidContainerRegistry.isFilledContainer(inputCopy.internal)) {
                        return new FluidStack(null);
                    }

                    net.minecraft.item.ItemStack output = FluidContainerRegistry.drainFluidContainer(inputCopy.internal);
                    onUpdate.accept(new ItemStack(output));

                    return FluidContainerRegistry.isFilledContainer(output) ? new FluidStack(null) : new FluidStack(FluidContainerRegistry.getFluidForFilledItem(inputCopy.internal));
                }
            };
        }
        return null;
    }

    /** Wrap Forge's IFluidHandler, do not use directly */
    static List<ITank> getTank(IFluidHandler internal, Facing dir) {
        ForgeDirection fd = dir == null ? ForgeDirection.UNKNOWN : dir.to();
        if (internal.getTankInfo(fd).length == 0) {
            return null;
        }
        return Arrays.stream(internal.getTankInfo(fd)).map(fluidTankInfo -> new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(fluidTankInfo.fluid);
            }

            @Override
            public int getCapacity() {
                return fluidTankInfo.capacity;
            }

            @Override
            public boolean allows(Fluid fluid) {
                return true;
                //TODO 1.7.10
                //return internal.getTankProperties()[0].canDrainFluidType(new net.minecraftforge.fluids.FluidStack(fluid.internal, 0)) ||
                //        internal.getTankProperties()[0].canFillFluidType(new net.minecraftforge.fluids.FluidStack(fluid.internal, 0));
            }

            @Override
            public int fill(FluidStack fluidStack, boolean simulate) {
                // BUG: This is a pretty fundamental problem with how forge's fluid API works.
                // IFluidHandler should really expose a list of distinct tanks
                return internal.fill(fd, fluidStack.internal, !simulate);
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                // BUG: This is a pretty fundamental problem with how forge's fluid API works.
                // IFluidHandler should really expose a list of distinct tanks
                return new FluidStack(internal.drain(fd, fluidStack.internal.amount, !simulate));
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
