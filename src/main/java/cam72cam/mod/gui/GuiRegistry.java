package cam72cam.mod.gui;

import cam72cam.mod.ModCore;
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

    public GuiRegistry(ModCore.Mod mod) {
        //TODO support for multiple mods using different ID ranges
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


    public GUIType register(String name, Supplier<IScreen> ctr) {
        Identifier id = new Identifier(name);
        registry.put(id, pkt -> ctr.get());
        return new GUIType(id);
    }

    public <T extends BlockEntity> GUIType registerBlock(Class<T> cls, Function<T, IScreen> ctr) {
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
        return new GUIType(id);
    }

    @Environment(EnvType.CLIENT)
    private static AbstractContainerScreen create(Container container) {
        return new ClientContainerBuilder((ServerContainerBuilder) container);
    }

    @Environment(EnvType.CLIENT)
    private void register(Identifier id) {
        net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry.INSTANCE.registerFactory(id.internal, GuiRegistry::create);
    }

    public <T extends Entity> GUIType registerEntityContainer(Class<T> cls, Function<T, IContainer> ctr) {
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

        return new GUIType(id);
    }

    public <T extends BlockEntity> GUIType registerBlockContainer(Class<T> cls, Function<T, IContainer> ctr) {
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

        return new GUIType(id);
    }

    public void openGUI(Player player, GUIType type) {
        OpenGuiPacket pkt = new OpenGuiPacket(type.id, new TagCompound(), player.getWorld());
        if (player.getWorld().isClient) {
            player.openGui(registry.get(type.id).apply(pkt));
        } else {
            pkt.sendToPlayer(player);
        }
    }

    public void openGUI(Player player, Entity ent, GUIType type) {
        ContainerProviderRegistry.INSTANCE.openContainer(type.id.internal, player.internal, buff -> {
            buff.writeUuid(ent.getUUID());
        });
    }

    public void openGUI(Player player, Vec3i pos, GUIType type) {
        if (registry.containsKey(type.id)) {
            TagCompound info = new TagCompound();
            info.setVec3i("pos", pos);
            OpenGuiPacket pkt = new OpenGuiPacket(type.id, info, player.getWorld());
            if (player.getWorld().isClient) {
                player.openGui(registry.get(type.id).apply(pkt));
            } else {
                pkt.sendToPlayer(player);
            }
        } else {
            ContainerProviderRegistry.INSTANCE.openContainer(type.id.internal, player.internal, buff -> {
                buff.writeBlockPos(pos.internal);
            });
        }
    }

    public static class GUIType {
        private final Identifier id;

        private GUIType(Identifier id) {
            this.id = id;
        }
    }

    private static class CreateEvent {
        final boolean isServer;
        final Player player;
        final int entityIDorX;
        final int y;
        final int z;

        private CreateEvent(boolean isServer, Player player, int entityIDorX, int y, int z) {
            this.isServer = isServer;
            this.player = player;
            this.entityIDorX = entityIDorX;
            this.y = y;
            this.z = z;
        }
    }
}
