package cam72cam.mod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cam72cam.mod.config.ConfigFile;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.Recipes;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.text.Command;
import cam72cam.mod.util.ModCoreCommand;
import cam72cam.mod.world.ChunkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IFutureReloadListener;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Unit;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.opengl.GL11;


/** UMC Mod, do not touch... */
@net.minecraftforge.fml.common.Mod(ModCore.MODID)
public class ModCore {
    public static final String MODID = "universalmodcore";
    public static final String NAME = "UniversalModCore";
    public static final String VERSION = "1.1.1";
    public static ModCore instance;
    public static boolean hasResources;
    private static boolean isInReload;

    private static List<Mod> mods = new ArrayList<>();
    private Logger logger;

    /** Register a mod, must happen before UMC is loaded! */
    public static void register(Mod ctr) {
        mods.add(ctr);
    }

    /** Called during Mod Construction phase */
    public ModCore() {
        System.out.println("Welcome to UniversalModCore!");
        instance = this;

        ModCore.register(new Internal());

        proxy.event(ModEvent.CONSTRUCT);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::postInit);
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverStarting);
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverStarted);

        MinecraftForge.EVENT_BUS.register(this);
    }

    /** INIT Phase (Forge) */
    public void preInit(FMLCommonSetupEvent event) {
        logger = LogManager.getLogger();
        proxy.event(ModEvent.INITIALIZE);
        hasResources = true;
    }

    private boolean hasSetup = false;

    /** SETUP Phase (Forge) */
    public void init(InterModEnqueueEvent event) {
        if(!hasSetup) {
            hasSetup = true;
            proxy.event(ModEvent.SETUP);
        }
    }

    /** FINALIZE Phase (Forge) */
    public void postInit(FMLLoadCompleteEvent event) {
        proxy.event(ModEvent.FINALIZE);
    }

	@SubscribeEvent
    public void serverStarting(FMLServerStartingEvent event) {
		// Formerly for command registration
    }

	/**
	 * <pre>
	 * Used to register commands.
	 * Moved from {@link ModCore#serverStarting serverStarting()}
	 * </pre>
	 * 
	 * @param event
	 */
	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		Command.registration(event.getDispatcher());
	}

    /** START Phase (Forge) */
    @SubscribeEvent
    public void serverStarted(FMLServerStartedEvent event) {
        proxy.event(ModEvent.START);
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
            return Paths.get(FMLPaths.CONFIGDIR.get().toString(), fname);
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

    private static Proxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);
    /** Hooked into forge's proxy system and fires off corresponding events */
    public static class Proxy {
        public Proxy() {
            proxy = this;
        }

        public void event(ModEvent event) {
            instance.mods.forEach(m -> event(event, m));
        }
        public void event(ModEvent event, Mod m) {
            m.commonEvent(event);
        }
    }

    public static class ClientProxy extends Proxy {
        static int MaxTextureSize = -1;

        public ClientProxy() {
            super();
            if (FMLPaths.CONFIGDIR.get() != null) { /* not a test environment */
                MaxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
                ModCore.info("Detected GL_MAX_TEXTURE_SIZE as: %s", MaxTextureSize);
            }
        }
        @Override
		public void event(ModEvent event, Mod m) {
            super.event(event, m);
            m.clientEvent(event);
        }
    }

    public static class ServerProxy extends Proxy {
        @Override
		public void event(ModEvent event, Mod m) {
            super.event(event, m);
            m.serverEvent(event);
        }
    }

    public static boolean isInReload() {
        return isInReload;
    }


    public static class Internal extends Mod {
        public int skipN = 0;

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
                    Packet.register(ModdedEntity.PassengerSeatPacket::new, PacketDirection.ServerToClient);
                    Packet.register(Mouse.MousePressPacket::new, PacketDirection.ClientToServer);
                    Command.register(new ModCoreCommand());
                    ConfigFile.sync(Config.class);
                    break;
                case INITIALIZE:
                    break;
                case SETUP:
                    CommonEvents.World.LOAD.subscribe(w -> w.increaseMaxEntityRadius(32));
                    break;
                case FINALIZE:
                    ChunkManager.setup();
                    break;
                case START:
                    break;
            }
        }

        public interface SynchronousResourceReloadListener extends IFutureReloadListener {
            @Override
			default CompletableFuture<Void> reload(IFutureReloadListener.IStage stage, IResourceManager resourceManager, IProfiler preparationsProfiler, IProfiler reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return stage.wait(Unit.INSTANCE).thenRunAsync(() -> {
                    this.apply(resourceManager);
                }, backgroundExecutor);
            }

            void apply(IResourceManager var1);
        }

        @Override
        public void clientEvent(ModEvent event) {
            switch (event) {
                case SETUP:
                    try {
                        Minecraft.getInstance().createSearchTrees();
                    } catch (Exception ex) {
                        ModCore.catching(ex);
                    }
                    /*
                    ((SimpleReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener((SynchronousResourceReloadListener)resourceManager -> {
                        if (skipN > 0) {
                            skipN--;
                            return;
                        }
                        ModCore.instance.mods.forEach(mod -> mod.clientEvent(ModEvent.RELOAD));
                        ClientEvents.fireReload();
                    });
                    */
                    //BlockRender.onPostColorSetup();
                    //ClientEvents.fireReload();
                    break;
            }

        }

        @Override
        public void serverEvent(ModEvent event) {
        }
    }

    static int i = 1;
    public static void testReload() {
        if (i % 10 == 0) { // 4 sheets, we fire on the last one
            ModCore.isInReload = true;
            proxy.event(ModEvent.RELOAD);
            ClientEvents.fireReload();
            ModCore.isInReload = false;
        }
        i++;
    }

    public static void genData(String MODID, GatherDataEvent event) {
        // src/main/resources/assets/immersiverailroading/
        Path langPath = Paths.get(
                event.getGenerator().getOutputFolder().getParent().getParent().toString(),
                "main", "resources", "assets", MODID, "lang");
        for (File path : langPath.toFile().listFiles()) {
            if (!path.getPath().endsWith(".lang")) {
                continue;
            }

            Path outPath = Paths.get(path.getParent(), path.toPath().getFileName().toString().toLowerCase().replace(".lang", ".json"));

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
                List<String> translations = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] splits = line.split("=", 2);
                    if (splits.length == 2) {
                        String key = splits[0];
                        String value = splits[1];

                        translations.add(String.format("\"%s\": \"%s\"", key, value));
                        translations.add(String.format("\"%s\": \"%s\"", key.replace(":", "."), value));
                        translations.add(String.format("\"%s\": \"%s\"", key.replace(".name", ""), value));
                        translations.add(String.format("\"%s\": \"%s\"", key.replace(".name", "").replace(":", "."), value));
                    }
                }
                String output = "{" + String.join(",", translations) + "}";
                System.out.println(outPath);
                System.out.println(output);
                Files.write(outPath, output.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        CommonEvents.Recipe.REGISTER.execute(Runnable::run);
        event.getGenerator().addProvider(new Recipes(event.getGenerator()));
        Fuzzy.register(event.getGenerator(), event.getExistingFileHelper());
    }

    public static void debug(String msg, Object... params) {
        if (Config.DebugLogging) {
            if (instance == null || instance.logger == null) {
                System.out.println("DEBUG: " + String.format(msg, params));
                return;
            }

            if (params.length != 0) {
                instance.logger.info(String.format(msg, params));
            } else {
                instance.logger.info(msg);
            }
        }
    }

    public static void info(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("INFO: " + String.format(msg, params));
            return;
        }
        if (params.length != 0) {
            instance.logger.info(String.format(msg, params));
        } else {
            instance.logger.info(msg);
        }
    }

    public static void warn(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("WARN: " + String.format(msg, params));
            return;
        }

        if (params.length != 0) {
            instance.logger.warn(String.format(msg, params));
        } else {
            instance.logger.warn(msg);
        }
    }

    public static void error(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("ERROR: " + String.format(msg, params));
            return;
        }

        if (params.length != 0) {
            instance.logger.error(String.format(msg, params));
        } else {
            instance.logger.error(msg);
        }
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

    /** Get a file for name in the UMC cache dir */
    public static File cacheFile(String name) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        if (configDir == null) {
            configDir = Paths.get(System.getProperty("java.io.tmpdir"), "minecraft");
        }
        File cacheDir = Paths.get(configDir.getParent().toString(), "cache", "universalmodcore").toFile();
        cacheDir.mkdirs();

        return new File(cacheDir, name);
    }
}
