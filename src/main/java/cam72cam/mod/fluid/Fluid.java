package cam72cam.mod.fluid;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;

public class Fluid {
    public static final int BUCKET_VOLUME = FluidVolume.BUCKET;
    private static Map<String, Fluid> registryCache = new HashMap<>();
    public static final Fluid WATER = getFluid("water");
    public static final Fluid LAVA = getFluid("lava");
    public final String ident;
    public final FluidKey internal;


    private Fluid(String ident, FluidKey fluid) {
        this.ident = ident;
        this.internal = fluid;
    }

    public static Fluid getFluid(String type) {
        if (!registryCache.containsKey(type)) {
            FluidKey fluid = FluidKeys.get(Registry.FLUID.get(new Identifier(type)));
            if (fluid == null) {
                return null;
            }
            registryCache.put(type, new Fluid(type, fluid));
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
