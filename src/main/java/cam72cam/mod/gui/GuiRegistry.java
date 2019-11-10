package cam72cam.mod.gui;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.container.ClientContainerBuilder;
import cam72cam.mod.gui.container.IContainer;
import cam72cam.mod.gui.container.ServerContainerBuilder;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.AbstractContainerScreen;
import net.minecraft.container.Container;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;


public class GuiRegistry {
    private static Map<Identifier, Function<OpenGuiPacket, IScreen>> registry = new HashMap<>();

    public GuiRegistry() {
    }

    public static class OpenGuiPacket extends Packet {
        private World world;

        public OpenGuiPacket() {

        }

        public OpenGuiPacket(Identifier guiType, TagCompound info, World world) {
            data.setString("id", guiType.toString());
            data.set("info", info);
            this.world = world;
        }

        @Override
        protected void handle() {
            getPlayer().openGui(registry.get(new Identifier(data.getString("id"))).apply(this));
        }

        World getPktWorld() {
            return world != null ? world : super.getWorld();
        }

        public TagCompound getInfo() {
            return data.get("info");
        }
    }

    @FunctionalInterface
    public interface GUI {
        void open(Player player);
    }

    @FunctionalInterface
    public interface EntityGUI {
        void open(Player player, Entity entity);
    }
    @FunctionalInterface
    public interface BlockGUI {
        void open(Player player, Vec3i pos);
    }

    public static GUI register(Identifier id, Supplier<IScreen> ctr) {
        registry.put(id, pkt -> ctr.get());
        return (player) -> {
            OpenGuiPacket pkt = new OpenGuiPacket(id, new TagCompound(), player.getWorld());
            if (player.getWorld().isClient) {
                player.openGui(registry.get(id).apply(pkt));
            } else {
                pkt.sendToPlayer(player);
            }
        };
    }

    public static <T extends BlockEntity> BlockGUI registerBlock(Class<T> cls, Function<T, IScreen> ctr) {
        Identifier id = new Identifier(cls.getName());
        registry.put(id, pkt -> {
            T entity = pkt.getPktWorld().getBlockEntity(pkt.getInfo().getVec3i("pos"), cls);
            if (entity == null) {
                return null;
            }
            IScreen screen = ctr.apply(entity);
            if (screen == null) {
                return null;
            }

            return screen;
        });
        return (player, pos) -> {
            if (registry.containsKey(id)) {
                TagCompound info = new TagCompound();
                info.setVec3i("pos", pos);
                OpenGuiPacket pkt = new OpenGuiPacket(id, info, player.getWorld());
                if (player.getWorld().isClient) {
                    player.openGui(registry.get(id).apply(pkt));
                } else {
                    pkt.sendToPlayer(player);
                }
            } else {
                ContainerProviderRegistry.INSTANCE.openContainer(id.internal, player.internal, buff -> {
                    buff.writeBlockPos(pos.internal);
                });
            }
        };
    }

    @Environment(EnvType.CLIENT)
    private static AbstractContainerScreen create(Container container) {
        return new ClientContainerBuilder((ServerContainerBuilder) container);
    }

    @Environment(EnvType.CLIENT)
    private static void register(Identifier id) {
        net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry.INSTANCE.registerFactory(id.internal, GuiRegistry::create);
    }

    public static <T extends Entity> EntityGUI registerEntityContainer(Class<T> cls, Function<T, IContainer> ctr) {
        Identifier id = new Identifier("container" + cls.getName());

        ContainerProviderRegistry.INSTANCE.registerFactory(id.internal, (syncId, containerId, player, buffer) -> {
            T entity = cam72cam.mod.world.World.get(player.getEntityWorld()).getEntity(buffer.readUuid(), cls);
            if (entity == null) {
                return null;
            }
            return new ServerContainerBuilder(player.inventory, ctr.apply(entity), syncId);
        });

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            register(id);
        }

        return (player, ent) -> {
            ContainerProviderRegistry.INSTANCE.openContainer(id.internal, player.internal, buff -> {
                buff.writeUuid(ent.getUUID());
            });
        };
    }

    public static <T extends BlockEntity> BlockGUI registerBlockContainer(Class<T> cls, Function<T, IContainer> ctr) {
        Identifier id = new Identifier("container" + cls.getName());

        ContainerProviderRegistry.INSTANCE.registerFactory(id.internal, (syncId, containerId, player, buffer) -> {
            T entity = cam72cam.mod.world.World.get(player.world).getBlockEntity(new Vec3i(buffer.readBlockPos()), cls);
            if (entity == null) {
                return null;
            }
            return new ServerContainerBuilder(player.inventory, ctr.apply(entity), syncId);
        });

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            register(id);
        }

        return (player, pos) -> {
            if (registry.containsKey(id)) {
                TagCompound info = new TagCompound();
                info.setVec3i("pos", pos);
                OpenGuiPacket pkt = new OpenGuiPacket(id, info, player.getWorld());
                if (player.getWorld().isClient) {
                    player.openGui(registry.get(id).apply(pkt));
                } else {
                    pkt.sendToPlayer(player);
                }
            } else {
                ContainerProviderRegistry.INSTANCE.openContainer(id.internal, player.internal, buff -> {
                    buff.writeBlockPos(pos.internal);
                });
            }
        };
    }
}
