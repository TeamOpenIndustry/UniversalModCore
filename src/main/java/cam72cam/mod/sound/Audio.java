package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

public class Audio {

    @SidedProxy(clientSide = "cam72cam.mod.sound.Audio$ClientProxy", serverSide = "cam72cam.mod.sound.Audio$ServerProxy", modId = ModCore.MODID)
    public static IAudioProxy proxy;

    public static void playSound(Vec3d pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
        proxy.playSound(pos, sound, category, volume, pitch);
    }

    public static void playSound(Vec3i pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
        proxy.playSound(new Vec3d(pos), sound, category, volume, pitch);
    }

    public static ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
        return proxy.loadSound(oggLocation, repeats, attenuationDistance, scale);
    }

    private interface IAudioProxy {
        ISound loadSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale);

        void playSound(Vec3d pos, StandardSound sound, SoundCategory category, float volume, float pitch);
    }

    public static class ServerProxy implements IAudioProxy {
        @Override
        public ISound loadSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
            throw new RuntimeException("Unable to play audio directly on the server...");
        }

        @Override
        public void playSound(Vec3d pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
            //Same as server world in MC
        }
    }

    public static class ClientProxy implements IAudioProxy {
        private static ModSoundManager soundManager;

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }

            Player player = MinecraftClient.getPlayer();
            World world = null;
            if (player != null) {
                world = player.getWorld();
                soundManager.tick();
            }

            if (world == null && soundManager != null && soundManager.hasSounds()) {
                soundManager.stop();
            }
        }

        @SubscribeEvent
        public void onSoundLoad(SoundLoadEvent event) {
            if (soundManager == null) {
                soundManager = new ModSoundManager(event.manager);
            } else {
                soundManager.handleReload(false);
            }
        }

        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load event) {
            soundManager.handleReload(true);
        }

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload event) {
            soundManager.stop();
        }

        public ISound loadSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
            return soundManager.createSound(oggLocation, repeats, attenuationDistance, scale);
        }

        @Override
        public void playSound(Vec3d pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
            MinecraftClient.getPlayer().getWorld().internal.playSound(pos.x, pos.y, pos.z, sound.event, volume, pitch, false);
        }
    }
}
