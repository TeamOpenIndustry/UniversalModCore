package cam72cam.mod;

import cam72cam.mod.config.ConfigFile;
import cam72cam.mod.entity.CustomSpawnPacket;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.render.BlockRender;
import cam72cam.mod.text.Command;
import cam72cam.mod.util.ModCoreCommand;
import cam72cam.mod.world.ChunkManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.resource.SynchronousResourceReloadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** UMC Mod, do not touch... */
public class ModCore implements ModInitializer {
    public static final String MODID = "universalmodcore";
    public static final String NAME = "UniversalModCore";
    public static final String VERSION = "1.0.1";
    public static ModCore instance;

    static List<Mod> mods = new ArrayList<>();
    private Logger logger;

    /** Register a mod, must happen before UMC is loaded! */
    public static void register(Mod ctr) {
        if (proxy == null) {
            proxy = new Proxy();
        }
        mods.add(ctr);
        proxy.event(ModEvent.CONSTRUCT, ctr);
    }

    /** Called during Mod Construction phase */
    public ModCore() {
        System.out.println("Welcome to UniversalModCore!");
        instance = this;

        logger = LogManager.getLogger("universalmodcore");
    }


    @Override
    public void onInitialize() {
        CommonEvents.Block.REGISTER.execute(Runnable::run);
        CommonEvents.Item.REGISTER.execute(Runnable::run);
        CommonEvents.Entity.REGISTER.execute(Runnable::run);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> proxy.event(ModEvent.START));
    }

    public void preInit() {
        mods = Collections.unmodifiableList(mods);
        proxy.event(ModEvent.INITIALIZE);
    }

    public void postInit() {
        proxy.event(ModEvent.SETUP);
        proxy.event(ModEvent.FINALIZE);
    }

    public static List<String> modIDs() {
        return mods.stream().filter(m -> !(m instanceof Internal)).map(Mod::modID).collect(Collectors.toList());
    }

    /** Implement this to create a UMC mod */
    public static abstract class Mod {
        public abstract String modID();

        /** Called both server and client side with a given event */
        public abstract void commonEvent(ModEvent event);
        /** Called client side with a given event */
        public abstract void clientEvent(ModEvent event);
        /** Called server side with a given event */
        public abstract void serverEvent(ModEvent event);

        /** Get config file for filename */
        public final Path getConfig(String fname) {
            return Paths.get(FabricLoader.getInstance().getConfigDir().toString(), fname);
        }

        /* Standard logging functions */

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

    static Proxy proxy;
    /** Hooked into fabric's proxy system and fires off corresponding events */
    public static class Proxy {
        private boolean isServer;
        private boolean isClient;

        public Proxy() {
            proxy = this;
            ModCore.register(new Internal());
        }

        public void enableClient() {
            this.isClient = true;
        }

        public void event(ModEvent event) {
            mods.forEach(m -> event(event, m));
        }
        public void event(ModEvent event, Mod m) {
            System.out.println(String.format("%s : %s", event.name(), m.modID()));
            m.commonEvent(event);
            if (event != ModEvent.CONSTRUCT) {
                if (isClient) {
                    m.clientEvent(event);
                }
                if (isServer) {
                    m.serverEvent(event);
                }
            }
        }

        public void enableServer() {
            this.isServer = true;
        }

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
                    Packet.register(ModdedEntity.PassengerPositionsPacket::new, PacketDirection.ServerToClient);
                    Packet.register(CustomSpawnPacket::new, PacketDirection.ServerToClient);
                    Packet.register(GuiRegistry.OpenGuiPacket::new, PacketDirection.ServerToClient);
                    Packet.register(ModdedEntity.PassengerSeatPacket::new, PacketDirection.ServerToClient);
                    Command.register(new ModCoreCommand());
                    ConfigFile.sync(Config.class);
                    break;
                case INITIALIZE:
                    ChunkManager.setup();
                    break;
                case SETUP:
                    CommonEvents.registerEvents();


                    //TODO World.getEntities
                    //World.MAX_ENTITY_RADIUS = Math.max(World.MAX_ENTITY_RADIUS, 32);

                    //GuiRegistry.registration();
                    break;
                case FINALIZE:
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
                        mods.forEach(mod -> mod.clientEvent(ModEvent.RELOAD));
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

    public static void catching(Throwable ex, String msg, Object... params) {
        error(msg, params);
        catching(ex);
    }

    public static void catching(Throwable ex) {
        if (instance == null || instance.logger == null) {
            ex.printStackTrace();
            return;
        }

        instance.logger.catching(ex);
    }

    public static <T> T runOn(Supplier<Supplier<T>> client, Supplier<Supplier<T>> server) {
        switch (FabricLoader.getInstance().getEnvironmentType()) {
            case CLIENT:
                return client.get().get();
            case SERVER:
                return server.get().get();
            default:
                throw new RuntimeException("Invalid");
        }
    }

    public static void execOn(Supplier<Runnable> client, Supplier<Runnable> server) {
        switch (FabricLoader.getInstance().getEnvironmentType()) {
            case CLIENT:
                client.get().run();
                break;
            case SERVER:
                server.get().run();
                break;
            default:
                throw new RuntimeException("Invalid");
        }
    }

    /** Get a file for name in the UMC cache dir */
    public static File cacheFile(String name) {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        if (gameDir == null) {
            gameDir = Paths.get(System.getProperty("java.io.tmpdir"), "minecraft");
        }
        File cacheDir = Paths.get(gameDir.toString(), "cache", "universalmodcore").toFile();
        cacheDir.mkdirs();

        return new File(cacheDir, name);
    }
}
