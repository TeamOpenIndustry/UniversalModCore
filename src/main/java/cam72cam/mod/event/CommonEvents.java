package cam72cam.mod.event;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.platform.RegisterAdvancementEvent;
import cam72cam.mod.event.platform.RegisterBlockTagEvent;
import cam72cam.mod.event.platform.RegisterRecipeEvent;
import cam72cam.mod.event.platform.RegisterItemTagEvent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.world.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraft.world.server.ServerWorld;

import java.util.function.Consumer;

/** Registry of events that fire off on both client and server.  Do not use directly! */
public class CommonEvents {
    private static void registerEvents() {
        cam72cam.mod.world.World.registerEvents();
        cam72cam.mod.entity.EntityRegistry.registerEvents();
        cam72cam.mod.gui.GuiRegistry.registerEvents();
    }

    public static final class World {
        public static final Event<Consumer<net.minecraft.world.World>> LOAD = new Event<>();
        public static final Event<Consumer<net.minecraft.world.World>> UNLOAD = new Event<>();
        public static final Event<Consumer<ServerWorld>> SAVE = new Event<>();
        public static final Event<Consumer<net.minecraft.world.World>> TICK = new Event<>();
        public static final Event<Consumer<IChunk>> LOAD_CHUNK = new Event<>();
    }

    public static final class Block {
        public static final Event<Runnable> REGISTER = new Event<>();
        public static final Event<EventBusForge.BlockBrokenEvent> BROKEN = new Event<>();
        public static final Event<Consumer<RegisterBlockTagEvent>> TAGS = new Event<>();
    }

    public static final class Tile {
        public static final Event<Runnable> REGISTER = new Event<>();
    }

    public static final class Item {
        public static final Event<Runnable> REGISTER = new Event<>();
        public static final Event<Consumer<RegisterItemTagEvent>> TAGS = new Event<>();
    }

    public static final class Recipe {
        public static final Event<Consumer<RegisterRecipeEvent>> REGISTER = new Event<>();
        public static final Event<Consumer<RegisterAdvancementEvent>> RECIPE_ADVENCEMENTS = new Event.TransientEvent<>();
    }

    public static final class Entity {
        public static final Event<Runnable> REGISTER = new Event<>();
        public static final Event<EventBusForge.EntityJoinEvent> JOIN = new Event<>();
    }

    public static final Event<Consumer<IForgeRegistry<ContainerType<?>>>> CONTAINER_REGISTRY = new Event<>();

    @Mod.EventBusSubscriber(modid = ModCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class EventBusForge {
        // World
        @SubscribeEvent
        public static void onWorldLoad(WorldEvent.Load event) {
            World.LOAD.execute(x -> x.accept((net.minecraft.world.World)event.getWorld()));
        }

        @SubscribeEvent
        public static void onWorldLoad(ChunkDataEvent.Load event) {
            World.LOAD_CHUNK.execute(x -> x.accept(event.getChunk()));
        }

        @SubscribeEvent
        public static void onWorldLoad(WorldEvent.Save event) {
            World.SAVE.execute(x -> x.accept((ServerWorld) event.getWorld()));
        }

        @SubscribeEvent
        public static void onWorldUnload(WorldEvent.Unload event) {
            World.UNLOAD.execute(x -> x.accept((net.minecraft.world.World)event.getWorld()));
        }

        @SubscribeEvent
        public static void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase == TickEvent.Phase.START && event.world != null) {
                World.TICK.execute(x -> x.accept(event.world));
            }
        }

        @FunctionalInterface
        public interface EntityJoinEvent {
            boolean onJoin(net.minecraft.world.World world, net.minecraft.entity.Entity entity);
        }
        @SubscribeEvent
        public static void onEntityJoin(EntityJoinWorldEvent event) {
            if (!Entity.JOIN.executeCancellable(x -> x.onJoin(event.getWorld(), event.getEntity()))) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onEntityTransfer(EntityEvent.EnteringChunk event) {
            if (event.getEntity() instanceof ModdedEntity) {
                ModdedEntity modded = (ModdedEntity) event.getEntity();
                cam72cam.mod.world.World.get(modded.level).tracker
                        .move(modded,
                              //Don't calculate Y in 1.16- as no corresponding event posted
                              ChunkPos.asLong(event.getOldChunkX(), 0, event.getOldChunkZ()),
                              ChunkPos.asLong(event.getNewChunkX(), 0, event.getNewChunkZ()));
            }
        }

        @FunctionalInterface
        public interface BlockBrokenEvent {
            boolean onBroken(net.minecraft.world.World world, BlockPos pos, PlayerEntity player);
        }
        @SubscribeEvent
        public static void onBlockBreakEvent(BlockEvent.BreakEvent event) {
            if (!Block.BROKEN.executeCancellable(x -> x.onBroken((net.minecraft.world.World)event.getWorld(), event.getPos(), event.getPlayer()))) {
                event.setCanceled(true);
            }
        }

    }

    @Mod.EventBusSubscriber(modid = ModCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class EventBusMod {
        static {
            registerEvents();
        }

        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<net.minecraft.block.Block> event) {
            Block.REGISTER.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void registerTiles(RegistryEvent.Register<net.minecraft.tileentity.TileEntityType<?>> event) {
            Tile.REGISTER.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<net.minecraft.item.Item> event) {
            Item.REGISTER.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
            Entity.REGISTER.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void registerContainers(RegistryEvent.Register<ContainerType<?>> event) {
            CONTAINER_REGISTRY.execute(x -> x.accept(event.getRegistry()));
        }

        @SubscribeEvent
        public static void registerBlockTags(RegisterBlockTagEvent event) {
            Block.TAGS.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void registerItemTags(RegisterItemTagEvent event) {
            Item.TAGS.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void registerRecipes(RegisterRecipeEvent event) {
            CommonEvents.Recipe.REGISTER.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void registerAdvancements(RegisterAdvancementEvent event) {
            CommonEvents.Recipe.RECIPE_ADVENCEMENTS.execute(x -> x.accept(event));
        }
    }
}
