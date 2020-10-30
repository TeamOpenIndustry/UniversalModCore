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
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Packet abstraction and registration
 * @see TagSerializer
 */
public abstract class Packet {
    private static final SimpleNetworkWrapper net = NetworkRegistry.INSTANCE.newSimpleChannel("cam72cam.mod");

    // Packet class name -> Packet Constructor
    private static final Map<String, Supplier<Packet>> types = new HashMap<>();

    static {
        // Client to server
        net.registerMessage(new Packet.Handler<>(), Message.class, 0, Side.CLIENT);
        // Server to client
        net.registerMessage(new Packet.Handler<>(), Message.class, 1, Side.SERVER);
    }

    // Received packet context
    private MessageContext ctx;
    // Received packet data
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
        types.put(sup.get().getClass().toString(), sup);
    }

    /** Called after deserialization */
    protected abstract void handle();

    /** Only valid during handle */
    protected final World getWorld() {
        if (ctx.side == Side.CLIENT) {
            return getPlayer().getWorld();
        }
        return world;
    }

    /** Only valid during handle */
    protected final Player getPlayer() {
        if (ctx.side == Side.CLIENT) {
            return MinecraftClient.getPlayer();
        }
        return player;
    }

    /** Send from server to all players around this pos */
    public void sendToAllAround(World world, Vec3d pos, double distance) {
        net.sendToAllAround(new Message(this),
                new NetworkRegistry.TargetPoint(world.getId(), pos.x, pos.y, pos.z, distance));
    }

    /** Send from server to any player who is within viewing (entity tracker update) distance of the entity */
    public void sendToObserving(Entity entity) {
        net.minecraft.entity.Entity internal = entity.internal;
        int syncDist = EntityRegistry.instance().lookupModSpawn(internal.getClass(), true).getTrackingRange();
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
        net.sendToAll(new Message(this));
    }

    /** Forge message construct.  Do not use directly */
    public static class Message implements IMessage {
        Packet packet;

        public Message() {
            // FORGE REFLECTION
        }

        public Message(Packet pkt) {
            this.packet = pkt;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            TagCompound data = new TagCompound(ByteBufUtils.readTag(buf));
            String cls = data.getString("cam72cam.mod.pktid");
            packet = types.get(cls).get();
            packet.data = data;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            TagCompound data = new TagCompound();
            data.setString("cam72cam.mod.pktid", packet.getClass().toString());
            try {
                TagSerializer.serialize(data, packet);
            } catch (SerializationException e) {
                ModCore.catching(e);
            }
            ByteBufUtils.writeTag(buf, data.internal);
        }
    }

    /** Forge message handler construct.  Do not use directly */
    public static class Handler<T extends Message> implements IMessageHandler<T, IMessage> {
        @Override
        public IMessage onMessage(T message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
            return null;
        }

        private void handle(T message, MessageContext ctx) {
            message.packet.ctx = ctx;
            World world = ctx.side == Side.CLIENT ? MinecraftClient.getPlayer().getWorld() : World.get(ctx.getServerHandler().playerEntity.world);
            try {
                TagSerializer.deserialize(message.packet.data, message.packet, world);
            } catch (SerializationException e) {
                ModCore.catching(e);
            }
            message.packet.handle();
        }
    }
}
