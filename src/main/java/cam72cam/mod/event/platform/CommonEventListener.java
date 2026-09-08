package cam72cam.mod.event.platform;


import cam72cam.mod.ModCore;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.world.ChunkPos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;

@Mod.EventBusSubscriber(modid = ModCore.MODID)
public class CommonEventListener {
    static {
        CommonEvents.registerEvents();
    }

    // World
    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        CommonEvents.World.LOAD.execute(x -> x.accept(event.getWorld()));
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        CommonEvents.World.UNLOAD.execute(x -> x.accept(event.getWorld()));
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            CommonEvents.World.TICK.execute(x -> x.accept(event.world));
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<net.minecraft.block.Block> event) {
        CommonEvents.Block.REGISTER.execute(Runnable::run);
    }

    @SubscribeEvent
    public static void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        if (!CommonEvents.Block.BROKEN.executeCancellable(x -> x.onBroken(event.getWorld(), event.getPos(), event.getPlayer()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<net.minecraft.item.Item> event) {
        CommonEvents.Item.REGISTER.execute(Runnable::run);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        CommonEvents.Recipe.REGISTER.execute(Runnable::run);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        CommonEvents.Entity.REGISTER.execute(Runnable::run);
    }
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent event) {
        if (!CommonEvents.Entity.JOIN.executeCancellable(x -> x.onJoin(event.getWorld(), event.getEntity()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityTransfer(EntityEvent.EnteringChunk event) {
        if (event.getEntity() instanceof ModdedEntity) {
            ModdedEntity modded = (ModdedEntity) event.getEntity();
            cam72cam.mod.world.World.get(modded.world).tracker
                    .move(modded,
                            //Don't calculate Y in 1.16- as no corresponding event posted
                          ChunkPos.asLong(event.getOldChunkX(), 0, event.getOldChunkZ()),
                          ChunkPos.asLong(event.getNewChunkX(), 0, event.getNewChunkZ()));
        }
    }

    @FunctionalInterface
    public interface BlockBrokenEvent {
        boolean onBroken(net.minecraft.world.World world, BlockPos pos, EntityPlayer player);
    }

    @FunctionalInterface
    public interface EntityJoinEvent {
        boolean onJoin(net.minecraft.world.World world, net.minecraft.entity.Entity entity);
    }
}
