package cam72cam.mod.fluid;

import net.minecraftforge.fluids.FluidRegistry;

import java.util.HashMap;
import java.util.Map;

public class Fluid {
    public static final int BUCKET_VOLUME = 1000;
    private static final Map<String, Fluid> registryCache = new HashMap<>();
    public static final Fluid WATER = getFluid("water");
    public static final Fluid LAVA = getFluid("lava");

    // Fluid Name/Ident
    public final String ident;
    // Reference to internal forge fluid
    public final net.minecraftforge.fluids.Fluid internal;

    private Fluid(String ident, net.minecraftforge.fluids.Fluid fluid) {
        this.ident = ident;
        this.internal = fluid;
    }

    public static Fluid getFluid(String type) {
        if (!registryCache.containsKey(type)) {
            net.minecraftforge.fluids.Fluid fluid = FluidRegistry.getFluid(type);
            if (fluid == null) {
                return null;
            }
            registryCache.put(type, new Fluid(type, fluid));
        }
        return registryCache.get(type);
    }

    public static Fluid getFluid(net.minecraftforge.fluids.Fluid fluid) {
        return getFluid(FluidRegistry.getFluidName(fluid));
    }

    public int getDensity() {
        return internal.getDensity();
    }

    public String toString() {
        return ident + " : " + internal.toString() + " : " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Fluid && ((Fluid)o).ident.equals(ident);
    }
}
