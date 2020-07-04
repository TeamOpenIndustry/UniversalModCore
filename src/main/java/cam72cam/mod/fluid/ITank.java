package cam72cam.mod.fluid;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.util.Facing;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    static List<ITank> getTank(IFluidHandler internal, Facing dir) {
        ForgeDirection fd = dir == null ? ForgeDirection.UNKNOWN : dir.to();
        if (internal.getTankInfo(fd).length == 0) {
            return null;
        }
        return IntStream.range(0, internal.getTankInfo(fd).length).mapToObj(id -> new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(internal.getTankInfo(fd)[id].fluid);
            }

            @Override
            public int getCapacity() {
                return internal.getTankInfo(fd)[id].capacity;
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

    FluidStack getContents();

    int getCapacity();

    boolean allows(Fluid fluid);

    int fill(FluidStack fluidStack, boolean simulate);

    FluidStack drain(FluidStack fluidStack, boolean simulate);

}
