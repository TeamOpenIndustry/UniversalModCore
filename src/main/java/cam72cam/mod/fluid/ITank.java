package cam72cam.mod.fluid;

import cam72cam.mod.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.function.Consumer;

public interface ITank {
    static ITank getTank(ItemStack inputCopy, Consumer<ItemStack> onUpdate) {
        if (!(inputCopy instanceof IFluidContainerItem)) {
            return null;
        }

        IFluidContainerItem internal = (IFluidContainerItem) inputCopy;
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
                ItemStack dup = inputCopy.copy();
                IFluidContainerItem temp = (IFluidContainerItem)dup;
                temp.fill(dup.internal, fluidStack.internal, true);
                onUpdate.accept(dup);

                return internal.fill(inputCopy.internal, fluidStack.internal, !simulate);
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                ItemStack dup = inputCopy.copy();
                IFluidContainerItem temp = (IFluidContainerItem)dup;
                temp.drain(dup.internal, fluidStack.internal.amount, true);
                onUpdate.accept(dup);

                return new FluidStack(internal.drain(inputCopy.internal, fluidStack.internal.amount, !simulate));
            }
        };
    }

    static ITank getTank(IFluidHandler internal) {
        return new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(internal.getTankInfo(ForgeDirection.UNKNOWN)[0].fluid);
            }

            @Override
            public int getCapacity() {
                return internal.getTankInfo(ForgeDirection.UNKNOWN)[0].capacity;
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
