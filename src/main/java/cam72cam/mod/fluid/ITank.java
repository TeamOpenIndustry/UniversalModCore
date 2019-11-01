package cam72cam.mod.fluid;

import cam72cam.mod.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.function.Consumer;

public interface ITank {
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

    static ITank getTank(IFluidHandler internal) {
        return new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(internal.getTankInfo(ForgeDirection.UNKNOWN).length == 0 ? null : internal.getTankInfo(ForgeDirection.UNKNOWN)[0].fluid);
            }

            @Override
            public int getCapacity() {
                return internal.getTankInfo(ForgeDirection.UNKNOWN).length == 0 ? 0 : internal.getTankInfo(ForgeDirection.UNKNOWN)[0].capacity;
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
                return internal.fill(ForgeDirection.UNKNOWN, fluidStack.internal, !simulate);
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                return new FluidStack(internal.drain(ForgeDirection.UNKNOWN, fluidStack.internal.amount, !simulate));
            }
        };
    }

    FluidStack getContents();

    int getCapacity();

    boolean allows(Fluid fluid);

    int fill(FluidStack fluidStack, boolean simulate);

    FluidStack drain(FluidStack fluidStack, boolean simulate);

}
