package cam72cam.mod.event.mixin;

import cam72cam.mod.event.CommonEvents;
import net.fabricmc.fabric.api.event.world.WorldTickCallback;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

@Mixin(World.class)
public class WorldMixin {
    static {
        WorldTickCallback.EVENT.register((world) -> CommonEvents.World.TICK.execute(x -> x.accept(world)));
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(LevelProperties levelProperties_1, DimensionType dimensionType_1, BiFunction<World, Dimension, ChunkManager> biFunction_1, Profiler profiler_1, boolean boolean_1, CallbackInfo info) {
        CommonEvents.World.LOAD.execute(x -> x.accept((World)(Object)this));
    }
}
