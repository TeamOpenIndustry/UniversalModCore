package cam72cam.mod.input;

import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import java.util.*;

public class Keyboard {
    private static Map<UUID, Vec3d> vecs = new HashMap<>();

    public static Vec3d getMovement(Player player) {
        return vecs.getOrDefault(player.getUUID(), Vec3d.ZERO);
    }

    @Environment(EnvType.CLIENT)
    public static void registerKey(String name, int keyCode, String category, Runnable handler) {
        FabricKeyBinding key = FabricKeyBinding.Builder.create(new Identifier(name), InputUtil.Type.KEYSYM, keyCode, category).build();
        KeyBindingRegistry.INSTANCE.register(key);
        ClientEvents.TICK.subscribe(() -> {
            if (key.isPressed()) {
                handler.run();
            }
        });
    }

    @Environment(EnvType.CLIENT)
    public static void registerClientEvents() {
        ClientEvents.TICK.subscribe(() -> {
            ClientPlayerEntity player = net.minecraft.client.MinecraftClient.getInstance().player;
            if (player == null) {
                return;
            }
            new MovementPacket(
                    player.getUuid(),
                    new Vec3d(player.input.movementSideways, 0, player.input.movementForward).scale(player.isSprinting() ? 0.4 : 0.2)
            ).sendToServer();
        });
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
}
