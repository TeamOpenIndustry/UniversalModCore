package cam72cam.mod.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Separate channel: Forge rejects peers without this patched UMC. */
public final class PassengerInputNetwork {
    private static final String VERSION="1";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath("universalmodcore", "passenger_input"),
        () -> VERSION, VERSION::equals, VERSION::equals);
    private record InputMessage(int seatId, int mask) {}
    public static void register() {
        CHANNEL.registerMessage(0, InputMessage.class,
            (m,b) -> { b.writeVarInt(m.seatId()); b.writeByte(m.mask()); },
            b -> new InputMessage(b.readVarInt(), b.readUnsignedByte()),
            (m,supplier) -> {
                var ctx=supplier.get();
                if (ctx.getDirection()==NetworkDirection.PLAY_TO_SERVER) {
                    ctx.enqueueWork(() -> {
                        ServerPlayer player=ctx.getSender();
                        if (player!=null && player.getVehicle() instanceof SeatEntity seat
                            && seat.getId()==m.seatId() && seat.hasPassenger(player)) {
                            seat.recordPassengerInput(player,m.mask());
                        }
                    });
                }
                ctx.setPacketHandled(true);
            });
    }
    public static void send(int seatId,int mask) {
        CHANNEL.sendToServer(new InputMessage(seatId,mask & 31));
    }
}
