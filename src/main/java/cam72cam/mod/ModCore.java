package cam72cam.mod;

import cam72cam.mod.config.ConfigFile;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.render.BlockRender;
import cam72cam.mod.text.Command;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cam72cam.mod.util.ModCoreCommand;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cam72cam.mod.world.ChunkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** UMC Mod, do not touch... */
@cpw.mods.fml.common.Mod(modid = ModCore.MODID, name = ModCore.NAME, version = ModCore.VERSION, acceptedMinecraftVersions = "[1.7.10,1.8)")
public class ModCore {
    public static final String MODID = "universalmodcore";
    public static final String NAME = "UniversalModCore";
    public static final String VERSION = "1.0.1";
    public static ModCore instance;

    private List<Mod> mods = new ArrayList<>();
    private Logger logger;

    /** Register a mod, must happen before UMC is loaded! */
    public static void register(Mod ctr) {
        instance.mods.add(ctr);
        proxy.event(ModEvent.CONSTRUCT, ctr);
    }

    /** Called during Mod Construction phase */
    public ModCore() {
        System.out.println("Welcome to UniversalModCore!");
        instance = this;
    }

    /** INIT Phase (Forge) */
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // LOCK MODS
        mods = Collections.unmodifiableList(mods);

        logger = event.getModLog();
        proxy.event(ModEvent.INITIALIZE);
    }

    /** SETUP Phase (Forge) */
    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.event(ModEvent.SETUP);
    }

    /** FINALIZE Phase (Forge) */
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.event(ModEvent.FINALIZE);
    }

    /** START Phase (Forge) */
    @EventHandler
    public void serverStarting(FMLServerStartedEvent event) {
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
            return Paths.get(Loader.instance().getConfigDir().toString(), fname);
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

    @SidedProxy(serverSide = "cam72cam.mod.ModCore$ServerProxy", clientSide = "cam72cam.mod.ModCore$ClientProxy", modId = ModCore.MODID)
    private static Proxy proxy;
    /** Hooked into forge's proxy system and fires off corresponding events */
    public static class Proxy {
        public Proxy() {
            proxy = this;
            ModCore.register(new Internal());
        }

        public void event(ModEvent event) {
            instance.mods.forEach(m -> event(event, m));
        }
        public void event(ModEvent event, Mod m) {
            m.commonEvent(event);
        }
    }

    public static class ClientProxy extends Proxy {
        public void event(ModEvent event, Mod m) {
            super.event(event, m);
            m.clientEvent(event);
        }
    }

    public static class ServerProxy extends Proxy {
        public void event(ModEvent event, Mod m) {
            super.event(event, m);
            m.serverEvent(event);
        }
    }


    private static void addHandler(Object handler) {
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }

    public static class Internal extends Mod {
        public int skipN = 2;

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
                    Packet.register(Player.MovementSync::new, PacketDirection.ClientToServer);
                    Packet.register(Player.MovementSync2EB::new, PacketDirection.ServerToClient);
                    Command.register(new ModCoreCommand());
                    ConfigFile.sync(Config.class);
                    break;
                case INITIALIZE:
                    addHandler(new CommonEvents.EventBus());
                    break;
                case SETUP:
                    World.MAX_ENTITY_RADIUS = Math.max(World.MAX_ENTITY_RADIUS, 32);

                    GuiRegistry.registration();
                    break;
                case FINALIZE:
                    ChunkManager.setup();
                    break;
                case START:
                    Command.registration();
                    break;
            }
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void clientEvent(ModEvent event) {
            switch (event) {
                case CONSTRUCT:
                    break;
                case INITIALIZE:
                    addHandler(new ClientEvents.ClientEventBus());
                    break;
                case SETUP:
                    if (Minecraft.getMinecraft().getResourceManager() instanceof SimpleReloadableResourceManager) {
                        // Lambda does not work here!  Don't simplify!
                        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new IResourceManagerReloadListener() {
                            @Override
                            public void onResourceManagerReload(IResourceManager p_110549_1_) {

                                if (skipN > 0) {
                                    skipN--;
                                    return;
                                }
                                ModCore.instance.mods.forEach(mod -> mod.clientEvent(ModEvent.RELOAD));
                            }
                        });
                    } else {
                        error("BAD RESOURCE MANAGER TYPE " + Minecraft.getMinecraft().getResourceManager());
                    }
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
}
