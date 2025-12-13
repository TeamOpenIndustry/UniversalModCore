package cam72cam.mod.event;

import cam72cam.mod.ModCore;
import cam72cam.mod.net.Packet;
import cam72cam.mod.render.GlobalRender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegisterEvent.RegisterHelper;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

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
        public static final Event<Consumer<RegisterCapabilitiesEvent>> REGISTER_CAPABILITY = new Event<>();
        public static final Event<EventBusForge.BlockBrokenEvent> BROKEN = new Event<>();
    }

    public static final class Tile {
        public static final Event<Consumer<RegisterHelper<BlockEntityType<?>>>> REGISTER = new Event<>();
    }

    public static final class Item {
        public static final Event<Consumer<RegisterHelper<net.minecraft.world.item.Item>>> REGISTER = new Event<>();
        public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModCore.MODID);
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

    public static final class Networking {
        public static final Event<Consumer<IPayloadRegistrar>> REGISTER_PACKET = new Event<>();
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
            //Since 1.18.2 this is called both server and client
            //We only want server side
            if (event.phase == TickEvent.Phase.START && event.level != null && !event.level.isClientSide()) {
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
            GlobalRender.registerClientEvents();
        }

        @SubscribeEvent
        public static void registerBlocks(RegisterEvent event) {
            event.register(BuiltInRegistries.BLOCK.key(), helper -> Block.REGISTER.execute(x -> x.accept(helper)));
            event.register(BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), helper -> Tile.REGISTER.execute(x -> x.accept(helper)));
            event.register(BuiltInRegistries.ITEM.key(), helper -> Item.REGISTER.execute(x -> x.accept(helper)));
            event.register(BuiltInRegistries.ENTITY_TYPE.key(), helper -> Entity.REGISTER.execute(x -> x.accept(helper)));
            event.register(BuiltInRegistries.MENU.key(), helper -> CONTAINER_REGISTRY.execute(x -> x.accept(helper)));
        }

        @SubscribeEvent
        public static void registerCapability(RegisterCapabilitiesEvent event) {
            Block.REGISTER_CAPABILITY.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void registerPacket(RegisterPayloadHandlerEvent event) {
            final IPayloadRegistrar registrar = event.registrar(ModCore.MODID)
                                                     .versioned(Packet.VERSION)
                                                     .optional();
            Networking.REGISTER_PACKET.execute(x -> x.accept(registrar));
        }
    }
}
