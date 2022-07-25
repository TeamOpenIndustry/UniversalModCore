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
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * Packet abstraction and registration
 * @see TagSerializer
 */
public abstract class Packet {
    private static final String VERSION = "1.0";
    private static final SimpleChannel net = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("universalmodcore", "cam72cam.mod"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals
    );
    // Packet class name -> Packet Constructor
    private static Map<String, Supplier<Packet>> types = new HashMap<>();

    static {
        net.registerMessage(0, Message.class, Message::toBytes, Message::new, (msg, ctx) -> {
            ctx.get().enqueueWork(() -> {
                msg.packet.ctx = ctx.get();
                World world = ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT ? MinecraftClient.getPlayer().getWorld() : World.get(ctx.get().getSender().world);
                try {
                    TagSerializer.deserialize(msg.packet.data, msg.packet, world);
                } catch (SerializationException e) {
                    ModCore.catching(e);
                }
                msg.packet.handle();
            });
            ctx.get().setPacketHandled(true);
        });
    }

    // Received packet context
    NetworkEvent.Context ctx;
    // Received packet data
    private TagCompound data = new TagCompound();

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
        types.put(sup.get().getClass().toString(), sup);
    }

    /** Called after deserialization */
    protected abstract void handle();

    /** Only valid during handle */
    protected final World getWorld() {
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            return getPlayer().getWorld();
        }
        return world;
    }

    /** Only valid during handle */
    protected final Player getPlayer() {
        return ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT ? MinecraftClient.getPlayer() : player;
    }

    /** Send from server to all players around this pos */
    public void sendToAllAround(World world, Vec3d pos, double distance) {
        net.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, distance, world.internal.getDimension().getType())), new Message(this));
    }

    /** Send from server to any player who is within viewing (entity tracker update) distance of the entity */
    public void sendToObserving(Entity entity) {
        net.minecraft.entity.Entity internal = entity.internal;
        int syncDist = entity.internal.getType().getTrackingRange();
        this.sendToAllAround(entity.getWorld(), entity.getPosition(), syncDist);
    }

    /** Send from client to server */
    public void sendToServer() {
        this.player = MinecraftClient.getPlayer();
        this.world = MinecraftClient.getPlayer().getWorld();
        net.sendToServer(new Message(this));
    }

    /** Broadcast to all players from server */
    public void sendToAll() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModCore.warn("Warning, trying to send %s client side!", this);
            return;
        }
        net.send(PacketDistributor.ALL.noArg(), new Message(this));
    }

	/** Send from server to player */
	public void sendToPlayer(Player player) {
		net.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player.internal), new Message(this));
	}

    /** Forge message construct.  Do not use directly */
    public static class Message {
        Packet packet;

        public Message(Packet pkt) {
            this.packet = pkt;
        }

        public Message(PacketBuffer buff) {
            fromBytes(buff);
        }

        public void fromBytes(PacketBuffer buf) {
            TagCompound data = new TagCompound(buf.readCompoundTag());
            String cls = data.getString("cam72cam.mod.pktid");
            packet = types.get(cls).get();
            packet.data = data;
        }

        public void toBytes(PacketBuffer buf) {
            packet.data.setString("cam72cam.mod.pktid", packet.getClass().toString());
            try {
                TagSerializer.serialize(packet.data, packet);

            } catch (SerializationException e) {
                ModCore.catching(e);
            }
            buf.writeCompoundTag(packet.data.internal);
        }
    }
}
