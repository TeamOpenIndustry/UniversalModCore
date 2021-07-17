package cam72cam.mod.net;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.server.PlayerStream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

/**
 * Packet abstraction and registration
 * @see TagSerializer
 */
public abstract class Packet {
    private static MinecraftServer server;
    static {
        // Probably should not do this...
        ServerTickCallback.EVENT.register(server -> Packet.server = server);
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
                ServerSidePacketRegistry.INSTANCE.register(ident, (ctx, buffer) -> {
                    Packet packet = sup.get();
                    packet.data = new TagCompound(buffer.readCompoundTag());
                    packet.player = new Player(ctx.getPlayer());
                    packet.world = packet.player.getWorld();
                    try {
                        TagSerializer.deserialize(packet.data, packet, packet.world);
                    } catch (SerializationException e) {
                        ModCore.catching(e);
                    }
                    ctx.getTaskQueue().execute(packet::handle);
                });
                break;
            case ServerToClient:
                if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
                    return;
                }
                ClientSidePacketRegistry.INSTANCE.register(ident, (ctx, buffer) -> {
                    Packet packet = sup.get();
                    packet.data = new TagCompound(buffer.readCompoundTag());
                    if (ctx.getPlayer() != null) {
                        packet.player = new Player(ctx.getPlayer());
                    }
                    packet.world = World.get(((ClientPlayNetworkHandler)ctx).getWorld());
                    try {
                        TagSerializer.deserialize(packet.data, packet, packet.world);
                    } catch (SerializationException e) {
                        ModCore.catching(e);
                    }
                    ctx.getTaskQueue().execute(packet::handle);
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
        PlayerStream.around(world.internal, pos.internal(), distance).forEach(x -> ((ServerPlayerEntity)x).networkHandler.sendPacket(new CustomPayloadS2CPacket(getIdent(), getBuffer())));
    }

    /** Send from server to any player who is within viewing (entity tracker update) distance of the entity */
    public void sendToObserving(Entity entity) {
        net.minecraft.entity.Entity internal = entity.internal;
        int syncDist = entity.internal.getType().getMaxTrackDistance();
        this.sendToAllAround(entity.getWorld(), entity.getPosition(), syncDist);
    }

    /** Send from client to server */
    public void sendToServer() {
        this.player = MinecraftClient.getPlayer();
        this.world = MinecraftClient.getPlayer().getWorld();
        ClientSidePacketRegistry.INSTANCE.sendToServer(new CustomPayloadC2SPacket(getIdent(), getBuffer()));
    }

    /** Broadcast to all players from server */
    public void sendToAll() {
        if (server == null) {
            return;
        }
        server.getPlayerManager().sendToAll(new CustomPayloadS2CPacket(getIdent(), getBuffer()));
    }

	/** Send from server to player */
	public void sendToPlayer(Player player) {
        if (server == null) {
            return;
        }
        ServerSidePacketRegistry.INSTANCE.sendToPlayer(player.internal, new CustomPayloadS2CPacket(getIdent(), getBuffer()));
    }
}
