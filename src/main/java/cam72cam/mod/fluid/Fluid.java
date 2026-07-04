package cam72cam.mod.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fluid {
    public static final int BUCKET_VOLUME = 1000;
    private static final Map<String, Fluid> registryCache = new HashMap<>();
    public static final Fluid WATER = getFluid("water");
    public static final Fluid LAVA = getFluid("lava");

    // Fluid Name/Ident
    public final String ident;

    // Reference to internal forge fluid
    public final List<net.minecraft.world.level.material.Fluid> internal;


    private Fluid(String ident, List<net.minecraft.world.level.material.Fluid> fluid) {
        this.ident = ident;
        this.internal = fluid;
    }

    public static Fluid getFluid(String type) {
        if (!registryCache.containsKey(type)) {
            List<net.minecraft.world.level.material.Fluid> fluids = new ArrayList<>();
            for (ResourceLocation key : BuiltInRegistries.FLUID.keySet()) {
                if (key.getPath().equals(type)) {
                    net.minecraft.world.level.material.Fluid fluid = BuiltInRegistries.FLUID.get(key);
                    if (!BuiltInRegistries.FLUID.getDefaultKey().equals(BuiltInRegistries.FLUID.getKey(fluid))) {
                        fluids.add(fluid);
                    }
                }
            }
            if (fluids.isEmpty()) {
                fluids.add(Fluids.EMPTY);
            }
            registryCache.put(type, new Fluid(type, fluids));
        }
        return registryCache.get(type);
    }

    public static Fluid getFluid(net.minecraft.world.level.material.Fluid fluid) {
        return getFluid(BuiltInRegistries.FLUID.getKey(fluid).getPath());
    }

    public int getDensity() {
        return internal.get(0).getFluidType().getDensity();
    }

    public String toString() {
        return ident + " : " + internal.get(0).toString() + " : " + super.toString();
    }
}
