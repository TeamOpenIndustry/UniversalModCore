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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Packet abstraction and registration
 * @see TagSerializer
 */
public abstract class Packet {
    private static final String VERSION = "1.0";
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

    /** How to register a packet (do in CONSTRUCT phase) */
    public static void register(Supplier<Packet> sup, PacketDirection dir) {
        types.put(sup.get().getClass().toString(), sup);
        ResourceLocation name = ResourceLocation.tryBuild(ModCore.MODID, sup.get().getClass().getName().toLowerCase(Locale.ROOT).replace("$", "."));
        CommonEvents.Networking.REGISTER_PACKET.subscribe(iPayloadRegistrar ->
                                iPayloadRegistrar.play(name, Message::new, handler -> {
                                    switch (dir) {
                                        case ClientToServer -> handler.server((msg, context) -> {
                                            World world1 = World.get(context.player().get().level());
											try {
												TagSerializer.deserialize(msg.packet.data, msg.packet, world1);
											} catch (SerializationException e) {
                                                ModCore.catching(e);
                                                return;
											}
                                            if (msg.packet.getPlayer() == null) {
                                                try {
                                                    throw new Exception(String.format("Invalid Packet %s: missing player", msg.packet.getClass()));
                                                } catch (Exception e) {
                                                    ModCore.catching(e);
                                                    return;
                                                }
                                            }
											msg.packet.handle();
                                        });
                                        case ServerToClient -> handler.client((msg, context) -> {
                                            World world1 = MinecraftClient.getPlayer().getWorld();
                                            try {
                                                TagSerializer.deserialize(msg.packet.data, msg.packet, world1);
                                            } catch (SerializationException e) {
                                                ModCore.catching(e);
                                                return;
                                            }
                                            if (msg.packet.getPlayer() == null) {
                                                try {
                                                    throw new Exception(String.format("Invalid Packet %s: missing player", msg.packet.getClass()));
                                                } catch (Exception e) {
                                                    ModCore.catching(e);
                                                    return;
                                                }
                                            }
                                            msg.packet.handle();
                                        });
                                    }
                                }));
    }

    /** Called after deserialization */
    protected abstract void handle();

    /** Only valid during handle */
    protected final World getWorld() {
        if (FMLEnvironment.dist.isClient()) {
            return getPlayer().getWorld();
        }
        return world;
    }

    /** Only valid during handle */
    protected final Player getPlayer() {
        return FMLEnvironment.dist.isClient()
               ? MinecraftClient.getPlayer()
               : player;
    }

    /** Send from server to all players around this pos */
    public void sendToAllAround(World world, Vec3d pos, double distance) {
        PacketDistributor.NEAR.with(new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, distance, world.internal.dimension()))
                              .send(new Message(this));
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
        PacketDistributor.SERVER.noArg().send(new Message(this));
    }

    /** Broadcast to all players from server */
    public void sendToAll() {
        PacketDistributor.ALL.noArg().send(new Message(this));
    }

	/** Send from server to player */
	public void sendToPlayer(Player player) {
        PacketDistributor.PLAYER.with((ServerPlayer) player.internal).send(new Message(this));
	}

    /** Forge message construct.  Do not use directly */
    public static class Message implements CustomPacketPayload {
        Packet packet;
        ResourceLocation location;

        public Message(Packet pkt) {
            this.packet = pkt;
            this.location = ResourceLocation.tryBuild(ModCore.MODID, pkt.getClass().getName().toLowerCase(Locale.ROOT));
        }

        public Message(FriendlyByteBuf buff) {
            fromBytes(buff);
        }

        public void fromBytes(FriendlyByteBuf buf) {
            TagCompound data = new TagCompound(buf.readNbt());
            String cls = data.getString("cam72cam.mod.pktid");
            packet = types.get(cls).get();
            packet.data = data;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            packet.data.setString("cam72cam.mod.pktid", packet.getClass().toString());
            try {
                TagSerializer.serialize(packet.data, packet);

            } catch (SerializationException e) {
                ModCore.catching(e);
            }
            buf.writeNbt(packet.data.internal);
        }

        @Override
        public ResourceLocation id() {
            return location;
        }
    }
}
