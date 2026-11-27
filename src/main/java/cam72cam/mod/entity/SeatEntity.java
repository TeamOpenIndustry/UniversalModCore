package cam72cam.mod.entity;

import cam72cam.mod.math.Vec3d;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.UUID;

/** Seat construct to make multiple riders actually work */
public class SeatEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final int INPUT_LEFT = 1;
    private static final int INPUT_RIGHT = 2;
    private static final int INPUT_FORWARD = 4;
    private static final int INPUT_BACK = 8;
    private static final int INPUT_SPRINT = 16;
    private static final int INPUT_MASK = 31;
    private UUID inputOwner;
    private long inputTick;
    private int inputMask;

    // Loaded independently of SeatEntity so channel registration does not create its EntityType early.
    public static final class PassengerInputNetwork {
        private static final String VERSION = "1";
        private static SimpleChannel channel;

        private record InputMessage(int seatId, int mask) {}

        public static void register() {
            // A separate channel makes Forge reject peers without passenger input support.
            channel = NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(ModCore.MODID, "passenger_input"),
                    () -> VERSION, VERSION::equals, VERSION::equals);
            channel.registerMessage(0, InputMessage.class,
                    (message, buffer) -> {
                        buffer.writeVarInt(message.seatId());
                        buffer.writeByte(message.mask());
                    },
                    buffer -> new InputMessage(buffer.readVarInt(), buffer.readUnsignedByte()),
                    (message, supplier) -> {
                        var context = supplier.get();
                        if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                            context.enqueueWork(() -> {
                                ServerPlayer player = context.getSender();
                                if (player != null && player.getVehicle() instanceof SeatEntity seat
                                        && seat.getId() == message.seatId() && seat.hasPassenger(player)) {
                                    seat.recordPassengerInput(player.getUUID(), seat.level().getGameTime(), message.mask());
                                }
                            });
                        }
                        context.setPacketHandled(true);
                    });
        }

        private static void send(int seatId, int mask) {
            channel.sendToServer(new InputMessage(seatId, mask & INPUT_MASK));
        }

        static int samplePassengerInput(UUID player, UUID owner, long now, long tick, int mask) {
            return player.equals(owner) && now >= tick && now - tick <= 10 ? mask : 0;
        }

        static Vec3d passengerMovement(int mask) {
            int x = ((mask & INPUT_LEFT) != 0 ? 1 : 0) - ((mask & INPUT_RIGHT) != 0 ? 1 : 0);
            int z = ((mask & INPUT_FORWARD) != 0 ? 1 : 0) - ((mask & INPUT_BACK) != 0 ? 1 : 0);
            double length = Math.hypot(x, z);
            if (length == 0) {
                return new Vec3d(0, 0, 0);
            }
            double speed = (mask & INPUT_SPRINT) != 0 ? 0.4 : 0.2;
            return new Vec3d(x * speed / length, 0, z * speed / length);
        }
    }

    private void recordPassengerInput(UUID owner, long tick, int mask) {
        inputOwner = owner;
        inputTick = tick;
        inputMask = mask & INPUT_MASK;
    }

    Vec3d getPassengerMovement(net.minecraft.world.entity.player.Player player) {
        int mask = PassengerInputNetwork.samplePassengerInput(
                player.getUUID(), inputOwner, level().getGameTime(), inputTick, inputMask);
        return PassengerInputNetwork.passengerMovement(mask);
    }

    static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ModCore.MODID, "seat");
    public static final EntityType<SeatEntity> TYPE = makeType();

    private static EntityType<SeatEntity> makeType() {
        EntityType.EntityFactory<SeatEntity> ctr = SeatEntity::new;
        EntityType<SeatEntity> et = EntityType.Builder.of(ctr, MobCategory.MISC)
                .setShouldReceiveVelocityUpdates(false)
                .setTrackingRange(512)
                .setUpdateInterval(20)
                .fireImmune()
                .setCustomClientFactory((msg, world) -> new SeatEntity(BuiltInRegistries.ENTITY_TYPE.byId(msg.getTypeId()), world))
                .build(SeatEntity.ID.toString());
        return et;
    }

    static {
        World.onTick(SeatEntity::ticker);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> ClientEvents.TICK_POST.subscribe(() -> {
            if (MinecraftClient.isReady()) {
                ticker(MinecraftClient.getPlayer().getWorld());
            }
            sendPassengerInput();
        }));
    }

    // Called only from the client tick subscription above.
    private static void sendPassengerInput() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null
                || !(minecraft.player.getVehicle() instanceof SeatEntity seat)) {
            return;
        }
        int mask = 0;
        if (minecraft.screen == null && minecraft.isWindowActive()) {
            var keys = minecraft.options;
            if (keys.keyLeft.isDown()) mask |= INPUT_LEFT;
            if (keys.keyRight.isDown()) mask |= INPUT_RIGHT;
            if (keys.keyUp.isDown()) mask |= INPUT_FORWARD;
            if (keys.keyDown.isDown()) mask |= INPUT_BACK;
            if (keys.keySprint.isDown()) mask |= INPUT_SPRINT;
        }
        PassengerInputNetwork.send(seat.getId(), mask);
    }

    private static void ticker(World world) {
        for (cam72cam.mod.entity.Entity entity : world.getEntities(e -> e.internal instanceof SeatEntity, cam72cam.mod.entity.Entity.class)) {
            List<Entity> passengers = entity.internal.getPassengers();
            if (!passengers.isEmpty()) {
                ((SeatEntity) entity.internal).updatePassengerPreTick(passengers.get(0));
            }
        }
    }

    // What it's a part of
    private UUID parent;
    // What is in the seat
    private UUID passenger;
    // If we should try to render the rider as standing or sitting (partial support!)
    boolean shouldSit = true;
    // If a passenger has mounted and then dismounted (if so, we can go away)
    private boolean hasHadPassenger = false;
    // ticks alive?
    private int ticks = 0;

    public SeatEntity(EntityType type, Level worldIn) {
        super(type, worldIn);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        TagCompound data = new TagCompound(compound);
        parent = data.getUUID("parent");
        passenger = data.getUUID("passenger");
        shouldSit = data.getBoolean("shouldSit");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        TagCompound data = new TagCompound(compound);
        data.setUUID("parent", parent);
        data.setUUID("passenger", passenger);
        data.setBoolean("shouldSit", shouldSit);
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    public void tick() {
        ticks ++;
        if (ticks < 5) {
            return;
        }

        if (parent == null) {
            ModCore.debug("No parent, goodbye");
            this.remove(RemovalReason.KILLED);
            return;
        }
        if (passenger == null) {
            ModCore.debug("No passenger, goodbye");
            this.remove(RemovalReason.KILLED);
            return;
        }

        if (getPassengers().isEmpty()) {
            if (this.ticks < 20) {
                if (!hasHadPassenger) {
                    cam72cam.mod.entity.Entity toRide = World.get(level()).getEntity(passenger, cam72cam.mod.entity.Entity.class);
                    if (toRide != null) {
                        ModCore.debug("FORCE RIDER");
                        toRide.internal.startRiding(this, true);
                        hasHadPassenger = true;
                    }
                }
            } else {
                ModCore.debug("No passengers, goodbye");
                this.remove(RemovalReason.KILLED);
                return;
            }
        }

        if (getParent() == null) {
            if (ticks > 20) {
                ModCore.debug("No parent found, goodbye");
                this.remove(RemovalReason.KILLED);
            }
        }
    }

    public void setup(ModdedEntity moddedEntity, Entity passenger) {
        this.parent = moddedEntity.getUUID();
        this.passenger = passenger.getUUID();
        this.setPos(moddedEntity.getX(), moddedEntity.getY(), moddedEntity.getZ());
    }

    public void moveTo(ModdedEntity moddedEntity) {
        this.parent = moddedEntity.getUUID();
    }

    public cam72cam.mod.entity.Entity getParent() {
        cam72cam.mod.entity.Entity linked = World.get(level()).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            return linked;
        }
        return null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0;
    }

    int lastUpdateTick = -1;
    //@Override
    public final void updatePassengerPreTick(net.minecraft.world.entity.Entity passenger) {
        if (lastUpdateTick != this.ticks) {
            lastUpdateTick = this.ticks;
            cam72cam.mod.entity.Entity linked = World.get(level()).getEntity(parent, cam72cam.mod.entity.Entity.class);
            if (linked != null && linked.internal instanceof ModdedEntity) {
                ((ModdedEntity) linked.internal).updateSeat(this);
            }
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return shouldSit;
    }

    @Override
    public final void removePassenger(net.minecraft.world.entity.Entity passenger) {
        inputOwner = null;
        inputMask = 0;
        cam72cam.mod.entity.Entity linked = World.get(level()).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            if (!(passenger instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected())) {
                //We want to preserve ModdedEntity's passenger data on player disconnect
                //TODO Does this have bug on dedicated server?
                ((ModdedEntity) linked.internal).removeSeat(this);
            }
        }
        super.removePassenger(passenger);
    }


    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity livingEntity) {
        return livingEntity.position();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public cam72cam.mod.entity.Entity getEntityPassenger() {
        if (!this.isAlive()) {
            return null;
        }
        if (this.getPassengers().size() == 0) {
            return null;
        }
        return World.get(level()).getEntity(getPassengers().get(0));
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        TagCompound data = new TagCompound();
        data.setUUID("parent", parent);
        data.setUUID("passenger", passenger);
        buffer.writeNbt(data.internal);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        TagCompound data = new TagCompound(additionalData.readNbt());
        parent = data.getUUID("parent");
        passenger = data.getUUID("passenger");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;
    }
}
