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
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.*;
import net.minecraftforge.network.payload.PayloadConnection;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Packet abstraction and registration
 * @see TagSerializer
 */
public abstract class Packet {
    public static final String VERSION = "1.0";
    // Packet id (related to Class Name) -> Packet Constructor
    private static final Map<String, Supplier<Packet>> packetFactories = new HashMap<>();
    private static final Map<String, PacketDirection> packetDirections = new HashMap<>();
    private static final Map<String, PacketProtocol> packetProtocols = new HashMap<>();

    private final String id = getClass().getName();
    // Version of the packet (if it differs from the normal "1.0" you can check it here)
    private String version = VERSION;

    // Received packet data
    private TagCompound data = new TagCompound();

    private static final Map<String, ResourceLocation> locations = new HashMap<>();
    private static final Map<String, CustomPacketPayload.Type<Message>> customPayloadTypes = new HashMap<>();
    private static final Map<String, Channel<CustomPacketPayload>> packetPayloadChannels = new HashMap<>();

    /**
     * So either forge or minecraft has a bug where it mixes up the player in the context handler...
     *
     * We now track player and world ourselves
     */
    @TagField("umcPlayer")
    private Player player;

    @TagField("umcWorld")
    private World world;

    /** Assuming that one wants to register it in play.
     * @see public static void Packet.register(Supplier<Packet> sup, PacketDirection dir, PacketProtocol protocol) */
    public static void register(Supplier<Packet> sup, PacketDirection dir) {
        register(sup, dir, PacketProtocol.PLAY);
    }

    /** How to register a packet (do in CONSTRUCT phase) */
    public static void register(Supplier<Packet> sup, PacketDirection dir, PacketProtocol protocol) {
        Packet packet = sup.get();
        if (packetFactories.containsKey(packet.id)) {
            //Already registered, goodbye
            return;
        }
        packetFactories.put(packet.id, sup);

        packetDirections.put(packet.id, dir);

        packetProtocols.put(packet.id, protocol);

        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(ModCore.MODID,
                packet.id.toLowerCase(Locale.ROOT).replace("$", "."));
        locations.put(packet.id, location);

        CustomPacketPayload.Type<Message> payloadType = CustomPacketPayload.createType(location);
        customPayloadTypes.put(packet.id, payloadType);

        // Build a channel for this packet. Each packet gets its own channel.
        PayloadConnection<CustomPacketPayload> packetPayloadConnection = ChannelBuilder.named(location).payloadChannel();
        BiConsumer<Message, CustomPayloadEvent.Context> func = (message, ctx) -> ctx.enqueueWork(() -> {
            World world = World.get(ctx.isServerSide() ?
                    Objects.requireNonNull(ctx.getSender()).level() : Minecraft.getInstance().level);
            try {
                TagSerializer.deserialize(message.packet.data, message.packet, world);
            } catch (SerializationException e) {
                ModCore.catching(e);
                return;
            }
            if (message.packet.getPlayer() == null) {
                try {
                    throw new Exception(
                            String.format("Invalid Packet %s: missing player", message.packet.id));
                } catch (Exception e) {
                    ModCore.catching(e);
                    return;
                }
            }
            message.packet.handle();
        });
        Channel<CustomPacketPayload> packetPayloadChannel;
        if (protocol == PacketProtocol.CONFIGURATION)
        {
            packetPayloadChannel = (switch(dir) {
                case ClientToServer -> packetPayloadConnection.configuration().serverbound();
                case ServerToClient -> packetPayloadConnection.configuration().clientbound();
                case Bidirectional -> packetPayloadConnection.configuration().bidirectional();
            }).add(payloadType, Message.configCodec, func).build();
        } else if (protocol == PacketProtocol.LOGIN)
        {
            packetPayloadChannel = (switch(dir) {
                case ClientToServer -> packetPayloadConnection.login().serverbound();
                case ServerToClient -> packetPayloadConnection.login().clientbound();
                case Bidirectional -> packetPayloadConnection.login().bidirectional();
            }).add(payloadType, Message.configCodec, func).build();
        } else
        {
            packetPayloadChannel = (switch(dir) {
                case ClientToServer -> packetPayloadConnection.play().serverbound();
                case ServerToClient -> packetPayloadConnection.play().clientbound();
                case Bidirectional -> packetPayloadConnection.play().bidirectional();
            }).add(payloadType, Message.playCodec, func).build();
        }
        packetPayloadChannels.put(packet.id, packetPayloadChannel);
    }

    /** Called after deserialization */
    protected abstract void handle();

    /** Only valid during handle */
    protected final World getWorld() {
        if (world == null && FMLLoader.getDist().isClient()) return getPlayer().getWorld();
        return world;
    }

    /** Only valid during handle */
    protected final Player getPlayer() {
        if (player == null && FMLLoader.getDist().isClient()) return MinecraftClient.getPlayer();
        return player;
    }

    /** Send from server to all players around this pos */
    public void sendToAllAround(World world, Vec3d pos, double distance) {
        Packet.packetPayloadChannels.get(this.id).send(new Message(this), PacketDistributor.NEAR
                .with(new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, distance, world.internal.dimension())));
    }

    /** Send from server to any player who is within viewing (entity tracker update) distance of the entity */
    public void sendToObserving(Entity entity) {
        int syncDist = entity.internal.getType().clientTrackingRange();
        this.sendToAllAround(entity.getWorld(), entity.getPosition(), syncDist);
    }

    /** Send from client to server */
    public void sendToServer() {
        this.player = MinecraftClient.getPlayer();
        this.world = MinecraftClient.getPlayer().getWorld();
        Packet.packetPayloadChannels.get(this.id).send(new Message(this), PacketDistributor.SERVER.noArg());
    }

    /** Broadcast to all players from server */
    public void sendToAll() {
        Packet.packetPayloadChannels.get(this.id).send(new Message(this), PacketDistributor.ALL.noArg());
    }

    /** Send from server to player */
    public void sendToPlayer(Player player) {
        Packet.packetPayloadChannels.get(this.id).send(new Message(this),
                PacketDistributor.PLAYER.with((ServerPlayer) player.internal));
    }

    /** Forge message construct.  Do not use directly */
    public static class Message implements CustomPacketPayload {
        Packet packet;

        public static StreamCodec<FriendlyByteBuf, Message> configCodec = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Message::write,
                Message::new
        );

        public static StreamCodec<RegistryFriendlyByteBuf, Message> playCodec = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Message::write,
                Message::new
        );

        public Message(Packet pkt) {
            this.packet = pkt;

            //Set up data in advance for single player
            packet.data.setString("cam72cam.mod.pktid", packet.id);
            packet.data.setString("cam72cam.mod.pkver", packet.version);
            try {
                TagSerializer.serialize(packet.data, packet);
            } catch (SerializationException e) {
                ModCore.catching(e);
            }
        }

        public Message(CompoundTag buff) {
            TagCompound data = new TagCompound(buff);
            String id = data.getString("cam72cam.mod.pktid");
            this.packet = packetFactories.get(id).get();
            this.packet.version = data.getString("cam72cam.mod.pkver");
            if (!VERSION.equals(packet.version))
                ModCore.warn("Incorrect packet version: Should be {}, Is {}", VERSION, packet.version);
            this.packet.data = data;
        }

        public static CompoundTag write(Message message) {
            return message.packet.data.internal;
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return Packet.customPayloadTypes.get(this.packet.id);
        }
    }
}
