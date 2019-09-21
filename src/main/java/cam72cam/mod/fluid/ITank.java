package cam72cam.mod.fluid;

import cam72cam.mod.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.function.Consumer;

public interface ITank {
    static ITank getTank(ItemStack inputCopy, Consumer<ItemStack> onUpdate) {
        IFluidHandler internal = FluidUtil.getFluidHandler(inputCopy.internal);
        if (internal == null) {
            return null;
        }
        return new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(internal.getTankProperties()[0].getContents());
            }

            @Override
            public int getCapacity() {
                return internal.getTankProperties()[0].getCapacity();
            }

            @Override
            public boolean allows(Fluid fluid) {
                return internal.getTankProperties()[0].canDrainFluidType(new net.minecraftforge.fluids.FluidStack(fluid.internal, 0)) ||
                        internal.getTankProperties()[0].canFillFluidType(new net.minecraftforge.fluids.FluidStack(fluid.internal, 0));
            }

            @Override
            public int fill(FluidStack fluidStack, boolean simulate) {
                IFluidHandler temp = FluidUtil.getFluidHandler(inputCopy.copy().internal);
                temp.fill(fluidStack.internal, true);
                onUpdate.accept(inputCopy); // TODO 1.10.2

                return internal.fill(fluidStack.internal, !simulate);
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                IFluidHandler temp = FluidUtil.getFluidHandler(inputCopy.copy().internal);
                temp.drain(fluidStack.internal, true);
                onUpdate.accept(inputCopy); // TODO 1.10.2

                return new FluidStack(internal.drain(fluidStack.internal, !simulate));
            }
        };
    }

    static ITank getTank(IFluidHandler internal) {
        return new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(internal.getTankProperties()[0].getContents());
            }

            @Override
            public int getCapacity() {
                return internal.getTankProperties()[0].getCapacity();
            }

            @Override
            public boolean allows(Fluid fluid) {
                return internal.getTankProperties()[0].canDrainFluidType(new net.minecraftforge.fluids.FluidStack(fluid.internal, 0)) ||
                        internal.getTankProperties()[0].canFillFluidType(new net.minecraftforge.fluids.FluidStack(fluid.internal, 0));
            }

            @Override
            public int fill(FluidStack fluidStack, boolean simulate) {
                return internal.fill(fluidStack.internal, !simulate);
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                return new FluidStack(internal.drain(fluidStack.internal, !simulate));
            }
        };
    }

    FluidStack getContents();

    int getCapacity();

    boolean allows(Fluid fluid);

    int fill(FluidStack fluidStack, boolean simulate);

    FluidStack drain(FluidStack fluidStack, boolean simulate);

}
