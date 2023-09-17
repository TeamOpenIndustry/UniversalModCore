package cam72cam.mod.event;

import cam72cam.mod.ModCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;

import java.util.function.Consumer;

/** Registry of events that fire off on both client and server.  Do not use directly! */
public class CommonEvents {
    private static void registerEvents() {
        cam72cam.mod.world.World.registerEvents();
        cam72cam.mod.entity.EntityRegistry.registerEvents();
        cam72cam.mod.gui.GuiRegistry.registerEvents();
    }

    public static final class World {
        public static final Event<Consumer<Level>> LOAD = new Event<>();
        public static final Event<Consumer<Level>> UNLOAD = new Event<>();
        public static final Event<Consumer<ServerLevel>> SAVE = new Event<>();
        public static final Event<Consumer<Level>> TICK = new Event<>();
        public static final Event<Consumer<ChunkAccess>> LOAD_CHUNK = new Event<>();
    }

    public static final class Block {
        public static final Event<Runnable> REGISTER = new Event<>();
        public static final Event<EventBusForge.BlockBrokenEvent> BROKEN = new Event<>();
    }

    public static final class Tile {
        public static final Event<Runnable> REGISTER = new Event<>();
    }

    public static final class Item {
        public static final Event<Runnable> REGISTER = new Event<>();
    }

    public static final class Recipe {
        public static final Event<Runnable> REGISTER = new Event<>();
    }

    public static final class Entity {
        public static final Event<Runnable> REGISTER = new Event<>();
        public static final Event<EventBusForge.EntityJoinEvent> JOIN = new Event<>();
    }

    public static final Event<Consumer<IForgeRegistry<MenuType<?>>>> CONTAINER_REGISTRY = new Event<>();

    public static final class Permissions {
        public static final Event<Consumer<PermissionGatherEvent.Nodes>> NODES = new Event<>();
    }

    @Mod.EventBusSubscriber(modid = ModCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class EventBusForge {
        // World
        @SubscribeEvent
        public static void onWorldLoad(WorldEvent.Load event) {
            World.LOAD.execute(x -> x.accept((Level)event.getWorld()));
        }

        @SubscribeEvent
        public static void onWorldLoad(ChunkDataEvent.Load event) {
            World.LOAD_CHUNK.execute(x -> x.accept(event.getChunk()));
        }

        @SubscribeEvent
        public static void onWorldLoad(WorldEvent.Save event) {
            World.SAVE.execute(x -> x.accept((ServerLevel) event.getWorld()));
        }

        @SubscribeEvent
        public static void onWorldUnload(WorldEvent.Unload event) {
            World.UNLOAD.execute(x -> x.accept((Level)event.getWorld()));
        }

        @SubscribeEvent
        public static void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase == TickEvent.Phase.START && event.world != null) {
                World.TICK.execute(x -> x.accept(event.world));
            }
        }

        @FunctionalInterface
        public interface EntityJoinEvent {
            boolean onJoin(Level world, net.minecraft.world.entity.Entity entity);
        }
        @SubscribeEvent
        public static void onEntityJoin(EntityJoinWorldEvent event) {
            if (!Entity.JOIN.executeCancellable(x -> x.onJoin(event.getWorld(), event.getEntity()))) {
                event.setCanceled(true);
            }
        }

        @FunctionalInterface
        public interface BlockBrokenEvent {
            boolean onBroken(Level world, BlockPos pos, Player player);
        }
        @SubscribeEvent
        public static void onBlockBreakEvent(BlockEvent.BreakEvent event) {
            if (!Block.BROKEN.executeCancellable(x -> x.onBroken((Level)event.getWorld(), event.getPos(), event.getPlayer()))) {
                event.setCanceled(true);
            }
        }


        @SubscribeEvent
        public static void registerContainers(PermissionGatherEvent.Nodes event) {
            Permissions.NODES.execute(x -> x.accept(event));
        }

    }

    @Mod.EventBusSubscriber(modid = ModCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class EventBusMod {
        static {
            registerEvents();
        }

        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<net.minecraft.world.level.block.Block> event) {
            Block.REGISTER.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void registerTiles(RegistryEvent.Register<BlockEntityType<?>> event) {
            Tile.REGISTER.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<net.minecraft.world.item.Item> event) {
            Item.REGISTER.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
            Entity.REGISTER.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void registerContainers(RegistryEvent.Register<MenuType<?>> event) {
            CONTAINER_REGISTRY.execute(x -> x.accept(event.getRegistry()));
        }
    }
}
