package cam72cam.mod.event;

import cam72cam.mod.ModCore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegisterEvent.RegisterHelper;
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
        public static final Event<Consumer<RegisterHelper<net.minecraft.world.level.block.Block>>> REGISTER = new Event<>();
        public static final Event<EventBusForge.BlockBrokenEvent> BROKEN = new Event<>();
    }

    public static final class Tile {
        public static final Event<Consumer<RegisterHelper<BlockEntityType<?>>>> REGISTER = new Event<>();
    }

    public static final class Item {
        public static final Event<Consumer<RegisterHelper<net.minecraft.world.item.Item>>> REGISTER = new Event<>();
        public static final Event<Consumer<RegisterHelper<CreativeModeTab>>> CREATIVE_TAB = new Event<>();
    }

    public static final class Recipe {
        public static final Event<Runnable> REGISTER = new Event<>();
    }

    public static final class Entity {
        public static final Event<Consumer<RegisterHelper<EntityType<?>>>> REGISTER = new Event<>();
        public static final Event<EventBusForge.EntityJoinEvent> JOIN = new Event<>();
    }

    public static final Event<Consumer<RegisterHelper<MenuType<?>>>> CONTAINER_REGISTRY = new Event<>();

    public static final class Permissions {
        public static final Event<Consumer<PermissionGatherEvent.Nodes>> NODES = new Event<>();
    }

    @Mod.EventBusSubscriber(modid = ModCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class EventBusForge {
        // World
        @SubscribeEvent
        public static void onWorldLoad(LevelEvent.Load event) {
            World.LOAD.execute(x -> x.accept((Level)event.getLevel()));
        }

        @SubscribeEvent
        public static void onChunkLoad(ChunkEvent.Load event) {
            World.LOAD_CHUNK.execute(x -> x.accept(event.getChunk()));
        }

        @SubscribeEvent
        public static void onLevelSave(LevelEvent.Save event) {
            World.SAVE.execute(x -> x.accept((ServerLevel) event.getLevel()));
        }

        @SubscribeEvent
        public static void onWorldUnload(LevelEvent.Unload event) {
            World.UNLOAD.execute(x -> x.accept((Level)event.getLevel()));
        }

        @SubscribeEvent
        public static void onWorldTick(TickEvent.LevelTickEvent event) {
            if (event.phase == TickEvent.Phase.START && event.level != null) {
                World.TICK.execute(x -> x.accept(event.level));
            }
        }

        @FunctionalInterface
        public interface EntityJoinEvent {
            boolean onJoin(Level world, net.minecraft.world.entity.Entity entity);
        }
        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            if (!Entity.JOIN.executeCancellable(x -> x.onJoin(event.getLevel(), event.getEntity()))) {
                event.setCanceled(true);
            }
        }

        @FunctionalInterface
        public interface BlockBrokenEvent {
            boolean onBroken(Level world, BlockPos pos, Player player);
        }
        @SubscribeEvent
        public static void onBlockBreakEvent(BlockEvent.BreakEvent event) {
            if (!Block.BROKEN.executeCancellable(x -> x.onBroken((Level)event.getLevel(), event.getPos(), event.getPlayer()))) {
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
        public static void registerBlocks(RegisterEvent event) {
            event.register(ForgeRegistries.Keys.BLOCKS, helper -> Block.REGISTER.execute(x -> x.accept(helper)));
            event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, helper -> Tile.REGISTER.execute(x -> x.accept(helper)));
            event.register(ForgeRegistries.Keys.ITEMS, helper -> Item.REGISTER.execute(x -> x.accept(helper)));
            event.register(ForgeRegistries.Keys.ENTITY_TYPES, helper -> Entity.REGISTER.execute(x -> x.accept(helper)));
            event.register(ForgeRegistries.Keys.MENU_TYPES, helper -> CONTAINER_REGISTRY.execute(x -> x.accept(helper)));
        }
    }
}
