package cam72cam.mod.net;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.world.World;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

import java.util.function.Supplier;

/**
 * Packet abstraction and registration
 * @see TagSerializer
 */
public abstract class Packet {
    private static MinecraftServer server;
    static {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> Packet.server = server);
    }

    private TagCompound data;

    /**
     * So either forge or minecraft has a bug where it mixes up the player in the context handler...
     *
     * We now track player and world ourselves
     */
    @TagField("umcPlayer")
    private Player player;

    @TagField("umcWorld")
    private World world;

    /** How to register a packet (do in CONSTRUCT phase) */
    public static void register(Supplier<Packet> sup, PacketDirection dir) {
        Identifier ident = sup.get().getIdent();
        switch (dir) {
            case ClientToServer:
                ServerPlayNetworking.registerGlobalReceiver(ident, (server, player, handler, buffer, sender) -> {
                    Packet packet = sup.get();
                    packet.data = new TagCompound(buffer.readCompoundTag());
                    packet.player = new Player(player);
                    packet.world = packet.player.getWorld();
                    try {
                        TagSerializer.deserialize(packet.data, packet, packet.world);
                    } catch (SerializationException e) {
                        ModCore.catching(e);
                    }
                    server.execute(packet::handle);
                });
                break;
            case ServerToClient:
                if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
                    return;
                }
                ClientPlayNetworking.registerGlobalReceiver(ident, (client, handler, buffer, sender) -> {
                    Packet packet = sup.get();
                    packet.data = new TagCompound(buffer.readCompoundTag());
                    packet.player = new Player(client.player);
                    packet.world = World.get(handler.getWorld());
                    try {
                        TagSerializer.deserialize(packet.data, packet, packet.world);
                    } catch (SerializationException e) {
                        ModCore.catching(e);
                    }
                    client.execute(packet::handle);
                });
                break;
        }
    }

    protected Identifier getIdent() {
        return new Identifier(ModCore.MODID, getClass().getName().replace("$", "_").toLowerCase());
    }

    /** Called after deserialization */
    protected abstract void handle();

    /** Only valid during handle */
    protected final World getWorld() {
        return world;
    }

    /** Only valid during handle */
    protected final Player getPlayer() {
        return player;
    }

    protected PacketByteBuf getBuffer() {
        PacketByteBuf buff = new PacketByteBuf(Unpooled.buffer());
        data = new TagCompound();
        try {
            TagSerializer.serialize(data, this);
        } catch (SerializationException e) {
            ModCore.catching(e);
        }
        buff.writeCompoundTag(data.internal);
        return buff;
    }


    /** Send from server to all players around this pos */
    public void sendToAllAround(World world, Vec3d pos, double distance) {
        double distSq = distance * distance;
        for (ServerPlayerEntity player : ((ServerWorld) world.internal).getPlayers(p -> p.squaredDistanceTo(pos.internal()) < distSq)) {
            ServerPlayNetworking.send(player, getIdent(), getBuffer());
        }
    }

    /** Send from server to any player who is within viewing (entity tracker update) distance of the entity */
    public void sendToObserving(Entity entity) {
        int syncDist = entity.internal.getType().getMaxTrackDistance();
        this.sendToAllAround(entity.getWorld(), entity.getPosition(), syncDist);
    }

    /** Send from client to server */
    public void sendToServer() {
        this.player = MinecraftClient.getPlayer();
        this.world = MinecraftClient.getPlayer().getWorld();
        ClientPlayNetworking.send(getIdent(), getBuffer());
    }

    /** Broadcast to all players from server */
    public void sendToAll() {
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, getIdent(), getBuffer());
        }
    }

	/** Send from server to player */
	public void sendToPlayer(Player player) {
        ServerPlayNetworking.send((ServerPlayerEntity) player.internal, getIdent(), getBuffer());
    }
}
