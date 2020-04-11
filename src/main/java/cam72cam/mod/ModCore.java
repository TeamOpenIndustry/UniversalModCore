package cam72cam.mod;

import cam72cam.mod.block.tile.BlockEntityUpdatePacket;
import cam72cam.mod.entity.CustomSpawnPacket;
import cam72cam.mod.config.ConfigFile;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.input.Keyboard;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.render.BlockRender;
import cam72cam.mod.text.Command;
import cam72cam.mod.util.ModCoreCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.resource.SynchronousResourceReloadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModCore implements ModInitializer {
    public static final String MODID = "universalmodcore";
    public static final String NAME = "UniversalModCore";
    public static final String VERSION = "0.1.0";
    public static ModCore instance;
    static List<Supplier<Mod>> modCtrs = new ArrayList<>();

    List<Mod> mods;
    private Logger logger;

    public static void register(Supplier<Mod> ctr) {
        modCtrs.add(ctr);
    }

    public ModCore() {
        System.out.println("Welcome to UniversalModCore!");
        instance = this;

        mods = modCtrs.stream().map(Supplier::get).collect(Collectors.toList());

        proxy.event(ModEvent.CONSTRUCT);
        logger = LogManager.getLogger("universalmodcore");
    }


    @Override
    public void onInitialize() {
        CommonEvents.Block.REGISTER.execute(Runnable::run);
        CommonEvents.Item.REGISTER.execute(Runnable::run);
        CommonEvents.Entity.REGISTER.execute(Runnable::run);

        ServerStartCallback.EVENT.register(server -> proxy.event(ModEvent.START));
    }

    public void preInit() {
        proxy.event(ModEvent.INITIALIZE);
    }

    public void postInit() {
        proxy.event(ModEvent.SETUP);
        proxy.event(ModEvent.FINALIZE);
    }

    public static List<String> modIDs() {
        return instance.mods.stream().map(Mod::modID).collect(Collectors.toList());
    }

    public static abstract class Mod {
        public abstract String modID();

        public abstract void commonEvent(ModEvent event);
        public abstract void clientEvent(ModEvent event);
        public abstract void serverEvent(ModEvent event);

        public final Path getConfig(String fname) {
            return Paths.get(FabricLoader.getInstance().getConfigDirectory().toString(), fname);
        }

        public static void debug(String msg, Object...params) {
            ModCore.debug(msg, params);
        }
        public static void info(String msg, Object...params) {
            ModCore.info(msg, params);
        }
        public static void warn(String msg, Object...params) {
            ModCore.warn(msg, params);
        }
        public static void error(String msg, Object...params) {
            ModCore.error(msg, params);
        }
        public static void catching(Throwable ex) {
            ModCore.catching(ex);
        }
    }

    static Proxy proxy = new Proxy();
    public static class Proxy {
        private boolean isServer;
        private boolean isClient;

        public Proxy() {
        }

        public void enableClient() {
            this.isClient = true;
        }

        public void enableServer() {
            this.isServer = true;
        }

        public void event(ModEvent event) {
            instance.mods.forEach(m -> m.commonEvent(event));
            if (event != ModEvent.CONSTRUCT) {
                if (isClient) {
                    instance.mods.forEach(m -> m.clientEvent(event));
                }
                if (isServer) {
                    instance.mods.forEach(m -> m.serverEvent(event));
                }
            }
        }
    }

    static {
        ModCore.register(Internal::new);
    }

    public static class Internal extends Mod {
        public int skipN = 1;

        @Override
        public String modID() {
            return "universalmodcoreinternal";
        }

        @Override
        public void commonEvent(ModEvent event) {
            switch (event) {
                case CONSTRUCT:
                    Packet.register(EntitySync.EntitySyncPacket::new, PacketDirection.ServerToClient);
                    Packet.register(Keyboard.MovementPacket::new, PacketDirection.ClientToServer);
                    Packet.register(ModdedEntity.PassengerPositionsPacket::new, PacketDirection.ServerToClient);
                    Packet.register(BlockEntityUpdatePacket::new, PacketDirection.ServerToClient);
                    Packet.register(CustomSpawnPacket::new, PacketDirection.ServerToClient);
                    Packet.register(GuiRegistry.OpenGuiPacket::new, PacketDirection.ServerToClient);
                    Command.register(new ModCoreCommand());
                    ConfigFile.sync(Config.class);
                    break;
                case SETUP:
                    CommonEvents.registerEvents();


                    //TODO World.getEntities
                    //World.MAX_ENTITY_RADIUS = Math.max(World.MAX_ENTITY_RADIUS, 32);

                    //GuiRegistry.registration();
                    break;
                case START:
                    Command.registration();
                    break;
            }
        }

        @Override
        public void clientEvent(ModEvent event) {
            switch (event) {
                case INITIALIZE:
                    break;
                case SETUP:

                    ((ReloadableResourceManager) MinecraftClient.getInstance().getResourceManager()).registerListener((SynchronousResourceReloadListener) manager -> {
                        if (skipN > 0) {
                            skipN--;
                            return;
                        }
                        ModCore.instance.mods.forEach(mod -> mod.clientEvent(ModEvent.RELOAD));
                        ClientEvents.fireReload();
                    });
                    BlockRender.onPostColorSetup();
                    ClientEvents.fireReload();
                    break;
            }

        }

        @Override
        public void serverEvent(ModEvent event) {
        }
    }

    public static void debug(String msg, Object... params) {
        if (Config.DebugLogging) {
            if (instance == null || instance.logger == null) {
                System.out.println("DEBUG: " + String.format(msg, params));
                return;
            }

            instance.logger.info(String.format(msg, params));
        }
    }

    public static void info(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("INFO: " + String.format(msg, params));
            return;
        }

        instance.logger.info(String.format(msg, params));
    }

    public static void warn(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("WARN: " + String.format(msg, params));
            return;
        }

        instance.logger.warn(String.format(msg, params));
    }

    public static void error(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("ERROR: " + String.format(msg, params));
            return;
        }

        instance.logger.error(String.format(msg, params));
    }

    public static void catching(Throwable ex) {
        if (instance == null || instance.logger == null) {
            ex.printStackTrace();
            return;
        }

        instance.logger.catching(ex);
    }
}
