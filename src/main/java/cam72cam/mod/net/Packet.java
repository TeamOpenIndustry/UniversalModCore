package cam72cam.mod.net;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.world.World;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Packet abstraction and registration
 * @see TagSerializer
 */
public abstract class Packet {
    public static final String VERSION = "1.0";
    // Packet class name -> Packet Constructor
    private static Map<String, Supplier<Packet>> types = new HashMap<>();

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

    //TODO Fix 1.20.4 desync issue
    /** How to register a packet (do in CONSTRUCT phase) */
    public static void register(Supplier<Packet> sup, PacketDirection dir) {
        String pktClass = sup.get().getClass().toString();
        if (types.containsKey(pktClass)) {
            //Already registered, goodbye
            return;
        }
        types.put(pktClass, sup);
        ResourceLocation name = ResourceLocation.tryBuild(ModCore.MODID, sup.get().getClass().getName().toLowerCase(Locale.ROOT).replace("$", "."));
        CustomPacketPayload.Type<Message> type = new CustomPacketPayload.Type<>(name);
        CommonEvents.Networking.REGISTER_PACKET.subscribe(reg -> {
            reg.commonBidirectional(type, Message.codec, (msg, context) -> {
                context.enqueueWork(() -> {
                    msg.packet.ctx = context;
                    Packet newPacket = copyFreshPacket(msg);
                    World world = (context.protocol().isPlay() && context.flow() == PacketFlow.CLIENTBOUND)
                                  ? MinecraftClient.getPlayer().getWorld() : World.get(context.player().level());
                    try {
                        TagSerializer.deserialize(msg.packet.data, newPacket, world);
                    } catch (SerializationException e) {
                        ModCore.catching(e);
                        return;
                    }
                    newPacket.ctx = context;
                    if (newPacket.getPlayer() == null) {
                        try {
                            throw new Exception(
                                    String.format("Invalid Packet %s: missing player", newPacket.getClass()));
                        } catch (Exception e) {
                            ModCore.catching(e);
                            return;
                        }
                    }
                    newPacket.handle();
                });
            });
        });
    }

    //Since 1.20.4 NeoForge will use the identical packet object for networking in singleplayer
    //Good optimization but bad for UMC
    //Create a new copy to avoid modifying original packet object
    private static Packet copyFreshPacket(Message msg) {
        Packet newPacket;
        try {
            Constructor<? extends Packet> constructor =
                    msg.packet.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            newPacket = constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return newPacket;
    }

    IPayloadContext ctx;

    /** Called after deserialization */
    protected abstract void handle();

    /** Only valid during handle */
    protected final World getWorld() {
        if (ctx.protocol().isPlay() && ctx.flow() == PacketFlow.CLIENTBOUND) {
            return getPlayer().getWorld();
        }
        return world;
    }

    /** Only valid during handle */
    protected final Player getPlayer() {
        return (ctx.protocol().isPlay() && ctx.flow() == PacketFlow.CLIENTBOUND)
               ? MinecraftClient.getPlayer()
               : player;
    }

    /** Send from server to all players around this pos */
    public void sendToAllAround(World world, Vec3d pos, double distance) {
        PacketDistributor.sendToPlayersNear((ServerLevel) world.internal, null, pos.x, pos.y, pos.z, distance, new Message(this));
    }

    /** Send from server to any player who is within viewing (entity tracker update) distance of the entity */
    public void sendToObserving(Entity entity) {
        net.minecraft.world.entity.Entity internal = entity.internal;
        int syncDist = entity.internal.getType().clientTrackingRange();
        this.sendToAllAround(entity.getWorld(), entity.getPosition(), syncDist);
    }

    /** Send from client to server */
    public void sendToServer() {
        this.player = MinecraftClient.getPlayer();
        this.world = MinecraftClient.getPlayer().getWorld();
        PacketDistributor.sendToServer(new Message(this));
    }

    /** Broadcast to all players from server */
    public void sendToAll() {
        PacketDistributor.sendToAllPlayers(new Message(this));
    }

    /** Send from server to player */
    public void sendToPlayer(Player player) {
        PacketDistributor.sendToPlayer((ServerPlayer) player.internal, new Message(this));
    }

    /** Forge message construct.  Do not use directly */
    public static class Message implements CustomPacketPayload {
        Packet packet;
        ResourceLocation location;
        CustomPacketPayload.Type<Message> type;

        public static StreamCodec<FriendlyByteBuf, Message> codec = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Message::write,
                Message::new
        );

        public Message(Packet pkt) {
            this.packet = pkt;
            initLocation();

            //Set up data in advance for single player
            packet.data.setString("cam72cam.mod.pktid", packet.getClass().toString());
            try {
                TagSerializer.serialize(packet.data, packet);

            } catch (SerializationException e) {
                ModCore.catching(e);
            }
        }

        public Message(CompoundTag buff) {
            fromTag(buff);
            initLocation();
        }

        private void initLocation() {
            this.location = ResourceLocation.fromNamespaceAndPath(ModCore.MODID, packet.getClass().getName().toLowerCase(Locale.ROOT).replace("$", "."));
            this.type = new Type<>(location);
        }

        public void fromTag(CompoundTag buf) {
            TagCompound data = new TagCompound(buf);
            String cls = data.getString("cam72cam.mod.pktid");
            packet = types.get(cls).get();
            packet.data = data;
        }

        public static CompoundTag write(Message message) {
            return message.packet.data.internal;
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return type;
        }
    }
}
