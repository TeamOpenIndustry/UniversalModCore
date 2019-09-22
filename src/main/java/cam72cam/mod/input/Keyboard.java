package cam72cam.mod.input;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;

import java.util.*;
import java.util.function.Consumer;

public class Keyboard {
    @SidedProxy(clientSide = "cam72cam.mod.input.Keyboard$ClientProxy", serverSide = "cam72cam.mod.input.Keyboard$ServerProxy", modId = ModCore.MODID)
    public static Proxy proxy;
    private static Map<UUID, Vec3d> vecs = new HashMap<>();

    /* Player Movement */
    private static Map<String, Consumer<Player>> keyFuncs = new HashMap<>();

    public static Vec3d getMovement(Player player) {
        return vecs.getOrDefault(player.getUUID(), Vec3d.ZERO);
    }

    public static void registerKey(String name, int keyCode, String category, Consumer<Player> handler) {
        keyFuncs.put(name, handler);
        proxy.registerKey(name, keyCode, category);
    }

    /* Key Bindings */

    public static class KeyboardListener {
        static List<KeyBinding> keys = new ArrayList<>();

        @SubscribeEvent
        public void onKeyInput(TickEvent.ClientTickEvent event) {
            EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
            if (player == null) {
                return;
            }
            new MovementPacket(
                    player.getUniqueID(),
                    new Vec3d(player.moveForward, 0, player.moveStrafing).scale(player.isSprinting() ? 0.4 : 0.2)
            ).sendToServer();

            for (KeyBinding key : keys) {
                if (key.getIsKeyPressed()) {
                    new KeyPacket(key.getKeyDescription()).sendToServer();
                }
            }
        }
    }

    public static class MovementPacket extends Packet {
        public MovementPacket() {

        }

        public MovementPacket(UUID id, Vec3d move) {
            data.setUUID("id", id);
            data.setVec3d("move", move);
            vecs.put(data.getUUID("id"), data.getVec3d("move"));
        }

        @Override
        protected void handle() {
            vecs.put(data.getUUID("id"), data.getVec3d("move"));
        }
    }

    public static abstract class Proxy {
        public abstract void registerKey(String name, int keyCode, String category);
    }

    public static class ClientProxy extends Proxy {
        @Override
        public void registerKey(String name, int keyCode, String category) {
            KeyBinding key = new KeyBinding(name, keyCode, category);
            ClientRegistry.registerKeyBinding(key);
            KeyboardListener.keys.add(key);
        }
    }

    public static class ServerProxy extends Proxy {
        @Override
        public void registerKey(String name, int keyCode, String category) {
            // NOP
        }
    }

    public static class KeyPacket extends Packet {
        public KeyPacket() {

        }

        public KeyPacket(String name) {
            data.setString("name", name);
        }

        @Override
        protected void handle() {
            keyFuncs.get(data.getString("name")).accept(getPlayer());
        }
    }
}
