package cam72cam.mod.fluid;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fluid {
    public static final int BUCKET_VOLUME = FluidVolume.BUCKET;
    private static final Map<String, Fluid> registryCache = new HashMap<>();

    public static final Fluid WATER = getFluid("water");
    public static final Fluid LAVA = getFluid("lava");

    // Fluid Name/Ident
    public final String ident;

    // Reference to internal fluid
    public final List<FluidKey> internal;


    private Fluid(String ident, List<FluidKey> fluid) {
        this.ident = ident;
        this.internal = fluid;
    }

    public static Fluid getFluid(String type) {
        if (!registryCache.containsKey(type)) {
            List<FluidKey> fluids = new ArrayList<>();
            for (Identifier key : Registry.FLUID.getIds()) {
                if (key.getPath().equals(type)) {
                    FluidKey fluid = FluidKeys.get(Registry.FLUID.get(key));
                    if (fluid != null && !Registry.FLUID.getDefaultId().equals(Registry.FLUID.get(key))) {
                        fluids.add(fluid);
                    }
                }
            }
            registryCache.put(type, new Fluid(type, fluids));
        }
        return registryCache.get(type);
    }

    public static Fluid getFluid(FluidKey fluid) {
        return getFluid(Registry.FLUID.getId(fluid.getRawFluid()).toString());
    }

    public int getDensity() {
        return 1000; // TODO 1.14.4
    }

    public String toString() {
        return ident + " : " + internal.toString() + " : " + super.toString();
    }
}
