package cam72cam.mod.gui;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.container.ClientContainerBuilder;
import cam72cam.mod.gui.container.IContainer;
import cam72cam.mod.gui.container.ServerContainerBuilder;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.ScreenBuilder;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/** GUI Registry for all types of GUIs */
public class GuiRegistry {
    // GUI ID -> GUI Constructor
    private static final Map<Identifier, Function<OpenGuiPacket, Pair<IScreen, Supplier<Boolean>>>> registry = new HashMap<>();

    private GuiRegistry() {
    }


    /** Handle to a GUI that can be opened both client and server side */
    @FunctionalInterface
    public interface GUI {
        void open(Player player);
    }

    /** Handle to a Entity GUI that can be opened both client and server side */
    @FunctionalInterface
    public interface EntityGUI<T extends Entity> {
        void open(Player player, T entity);
    }

    /** Handle to a Block GUI that can be opened both client and server side */
    @FunctionalInterface
    public interface BlockGUI {
        void open(Player player, Vec3i pos);
    }

    /** Register a standalone GUI */
    public static GUI register(Identifier id, Supplier<IScreen> ctr) {
        registry.put(id, pkt -> Pair.of(ctr.get(), () -> true));
        return (player) -> {
            OpenGuiPacket pkt = new OpenGuiPacket(id, new TagCompound(), player.getWorld());
            ModCore.execOn(
                    () -> () -> MinecraftClient.getInstance().openScreen(new ScreenBuilder(registry.get(id).apply(pkt))),
                    () -> () -> pkt.sendToPlayer(player)
            );
        };
    }

    /** Register a Block based GUI */
    public static <T extends BlockEntity> BlockGUI registerBlock(Class<T> cls, Function<T, IScreen> ctr) {
        Identifier id = new Identifier(cls.getName());

        registry.put(id, pkt -> {
            Supplier<T> entSupplier = () -> pkt.getPktWorld().getBlockEntity(pkt.getInfo().getVec3i("pos"), cls);
            T entity = entSupplier.get();
            if (entity == null) {
                return null;
            }
            return Pair.of(
                    ctr.apply(entity),
                    () -> entSupplier.get() == entity
            );
        });
        return (player, pos) -> {
            TagCompound info = new TagCompound();
            info.setVec3i("pos", pos);
            OpenGuiPacket pkt = new OpenGuiPacket(id, info, player.getWorld());

            ModCore.execOn(
                    () -> () -> MinecraftClient.getInstance().openScreen(new ScreenBuilder(registry.get(id).apply(pkt))),
                    () -> () -> pkt.sendToPlayer(player)
            );
        };
    }

    /** Register a Block based Container */
    public static <T extends BlockEntity> BlockGUI registerBlockContainer(Class<T> cls, Function<T, IContainer> ctr) {
        Identifier id = new Identifier("container" + cls.getName());

        ContainerProviderRegistry.INSTANCE.registerFactory(id.internal, (syncId, containerId, player, buffer) -> {
            Supplier<T> entSupplier = () -> World.get(player.world).getBlockEntity(new Vec3i(buffer.readBlockPos()), cls);
            T entity = entSupplier.get();
            if (entity == null) {
                return null;
            }
            return new ServerContainerBuilder(player.inventory, ctr.apply(entity), syncId, () -> entSupplier.get() == entity);
        });

        ModCore.execOn(
                () -> () -> net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry.INSTANCE.registerFactory(
                        id.internal,
                        (factory) -> new ClientContainerBuilder((ServerContainerBuilder) factory)
                ),
                () -> () -> {}
        );

        return (player, pos) -> {
            if (!(player.internal instanceof ServerPlayerEntity)) {
                System.out.println("PROBS SHOULD SEND PKT");
                return;
            }
            ContainerProviderRegistry.INSTANCE.openContainer(id.internal, player.internal, buff -> {
                buff.writeBlockPos(pos.internal());
            });
        };
    }

    /** Register a Entity based GUI */
    public static <T extends Entity> EntityGUI<T> registerEntity(Class<T> cls, Function<T, IScreen> ctr) {
        Identifier id = new Identifier(cls.getName());

        registry.put(id, pkt -> {
            Supplier<T> entSupplier = () -> pkt.getInfo().getEntity("ent", pkt.getPktWorld(), cls);
            T entity = entSupplier.get();
            if (entity == null) {
                return null;
            }
            return Pair.of(
                    ctr.apply(entity),
                    () -> entSupplier.get() == entity
            );
        });
        return (player, ent) -> {
            TagCompound info = new TagCompound();
            info.setEntity("ent", ent);
            OpenGuiPacket pkt = new OpenGuiPacket(id, info, player.getWorld());

            ModCore.execOn(
                    () -> () -> MinecraftClient.getInstance().openScreen(new ScreenBuilder(registry.get(id).apply(pkt))),
                    () -> () -> pkt.sendToPlayer(player)
            );
        };
    }

    /** Register a Entity based Container */
    public static <T extends Entity> EntityGUI<T> registerEntityContainer(Class<T> cls, Function<T, IContainer> ctr) {
        Identifier id = new Identifier("container" + cls.getName());

        ContainerProviderRegistry.INSTANCE.registerFactory(id.internal, (syncId, containerId, player, buffer) -> {
            Supplier<T> entSupplier = () -> cam72cam.mod.world.World.get(player.getEntityWorld()).getEntity(buffer.readUuid(), cls);
            T entity = entSupplier.get();
            if (entity == null) {
                return null;
            }
            return new ServerContainerBuilder(player.inventory, ctr.apply(entity), syncId, () -> entSupplier.get() == entity);
        });

        ModCore.execOn(
                () -> () -> net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry.INSTANCE.registerFactory(
                        id.internal,
                        (factory) -> new ClientContainerBuilder((ServerContainerBuilder) factory)
                ),
                () -> () -> {}
        );

        return (player, ent) -> {
            if (!(player.internal instanceof ServerPlayerEntity)) {
                System.out.println("PROBS SHOULD SEND PKT");
                return;
            }
            ContainerProviderRegistry.INSTANCE.openContainer(id.internal, player.internal, buff -> {
                buff.writeUuid(ent.getUUID());
            });
        };
    }


    public static class OpenGuiPacket extends Packet {
        @TagField
        private String id;
        @TagField
        private TagCompound info;
        @TagField
        private World world;

        public OpenGuiPacket() {

        }

        public OpenGuiPacket(Identifier guiType, TagCompound info, World world) {
            this.id = guiType.toString();
            this.info = info;
            this.world = world;
        }

        @Override
        @Environment(EnvType.CLIENT)
        protected void handle() {
            MinecraftClient.getInstance().openScreen(new ScreenBuilder(registry.get(new Identifier(id)).apply(this)));
        }

        World getPktWorld() {
                      return world != null ? world : super.getWorld();
                                                                      }

        public TagCompound getInfo() {
                               return info;
                                           }
    }
}
