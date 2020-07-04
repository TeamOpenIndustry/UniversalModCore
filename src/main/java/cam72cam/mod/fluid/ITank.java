package cam72cam.mod.fluid;

import cam72cam.mod.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface ITank {
    static ITank getTank(ItemStack inputCopy, Consumer<ItemStack> onUpdate) {
        IFluidHandlerItem internal = FluidUtil.getFluidHandler(inputCopy.internal);
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
                IFluidHandlerItem temp = FluidUtil.getFluidHandler(inputCopy.copy().internal);
                temp.fill(fluidStack.internal, true);
                onUpdate.accept(new ItemStack(temp.getContainer()));

                return internal.fill(fluidStack.internal, !simulate);
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                IFluidHandlerItem temp = FluidUtil.getFluidHandler(inputCopy.copy().internal);
                temp.drain(fluidStack.internal, true);
                onUpdate.accept(new ItemStack(temp.getContainer()));

                return new FluidStack(internal.drain(fluidStack.internal, !simulate));
            }
        };
    }

    static List<ITank> getTank(IFluidHandler internal) {
        return IntStream.range(0, internal.getTankProperties().length).mapToObj(i -> new ITank() {
            @Override
            public FluidStack getContents() {
                return new FluidStack(internal.getTankProperties()[i].getContents());
            }

            @Override
            public int getCapacity() {
                return internal.getTankProperties()[i].getCapacity();
            }

            @Override
            public boolean allows(Fluid fluid) {
                return internal.getTankProperties()[i].canDrainFluidType(new net.minecraftforge.fluids.FluidStack(fluid.internal, 0)) ||
                        internal.getTankProperties()[i].canFillFluidType(new net.minecraftforge.fluids.FluidStack(fluid.internal, 0));
            }

            @Override
            public int fill(FluidStack fluidStack, boolean simulate) {
                // BUG: This is a pretty fundamental problem with how forge's fluid API works.
                // IFluidHandler should really expose a list of distinct tanks
                return internal.fill(fluidStack.internal, !simulate);
            }

            @Override
            public FluidStack drain(FluidStack fluidStack, boolean simulate) {
                // BUG: This is a pretty fundamental problem with how forge's fluid API works.
                // IFluidHandler should really expose a list of distinct tanks
                return new FluidStack(internal.drain(fluidStack.internal, !simulate));
            }
        }).collect(Collectors.toList());
    }

    FluidStack getContents();

    int getCapacity();

    boolean allows(Fluid fluid);

    int fill(FluidStack fluidStack, boolean simulate);

    FluidStack drain(FluidStack fluidStack, boolean simulate);

}
