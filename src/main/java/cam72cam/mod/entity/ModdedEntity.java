package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.custom.*;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.*;
import cam72cam.mod.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModdedEntity extends Entity implements IEntityAdditionalSpawnData {
    private cam72cam.mod.entity.Entity self;
    private EntitySettings settings;

    @TagField(value = "passengers", mapper = PassengerMapper.class)
    private Map<UUID, Vec3d> passengerPositions = new HashMap<>();

    private List<SeatEntity> seats = new ArrayList<>();

    private IWorldData iWorldData;
    private ITickable iTickable;
    private IClickable iClickable;
    private IKillable iKillable;
    private IRidable iRidable;
    private ICollision iCollision;

    public ModdedEntity(EntityType type, World world, Supplier<cam72cam.mod.entity.Entity> ctr, EntitySettings settings) {
        super(type, world);

        super.preventEntitySpawning = true;

        self = ctr.get();
        self.setup(this);
        this.settings = settings;

        iWorldData = IWorldData.get(self);
        iTickable = ITickable.get(self);
        iClickable = IClickable.get(self);
        iKillable = IKillable.get(self);
        iRidable = IRidable.get(self);
        iCollision = ICollision.get(self);
    }

    public cam72cam.mod.entity.Entity getSelf() {
        return self;
    }

    /* IWorldData */

    @Override
    public final void readAdditional(CompoundNBT compound) {
        load(new TagCompound(compound));
    }

    private final void load(TagCompound data) {
        try {
            TagSerializer.deserialize(data, this);
            TagSerializer.deserialize(data, self);
        } catch (SerializationException e) {
            ModCore.catching(e, "Error during entity load: %s - %s", self, data);
        }
        iWorldData.load(data);
    }

    @Override
    public void writeAdditional(CompoundNBT compound) {
        save(new TagCompound(compound));
    }

    private final void save(TagCompound data) {
        try {
            TagSerializer.serialize(data, self);
            TagSerializer.serialize(data, this);
        } catch (SerializationException e) {
            ModCore.catching(e, "Error during entity save: %s - %s", this, self);
        }
        iWorldData.save(data);
    }

    /* ISpawnData */

    @Override
    public final void readSpawnData(PacketBuffer additionalData) {
        TagCompound data = new TagCompound(additionalData.readCompoundTag());
        load(data);
        try {
            self.sync.receive(data.get("sync"));
        } catch (SerializationException e) {
            ModCore.catching(e, "Invalid sync payload %s for %s", data.get("sync"), this);
        }
    }

    @Override
    public final void writeSpawnData(PacketBuffer buffer) {
        TagCompound data = new TagCompound();
        data.set("sync", self.sync);
        save(data);
        buffer.writeCompoundTag(data.internal);
    }

    /* ITickable */

    @Override
    public final void tick() {
        iTickable.onTick();
        try {
            self.sync.send();
        } catch (SerializationException e) {
            ModCore.catching(e, "Unable to send sync data for %s - %s", this, self.sync);
        }

        if (!seats.isEmpty()) {
            seats.removeAll(seats.stream().filter(x -> !x.isAlive()).collect(Collectors.toList()));
            seats.forEach(seat -> seat.setPosition(posX, posY, posZ));
        }
    }

    /* Player Interact */

    @Override
    public final boolean processInitialInteract(PlayerEntity player, net.minecraft.util.Hand hand) {
        return iClickable.onClick(new Player(player), Hand.from(hand)) == ClickResult.ACCEPTED;
    }

    /* Death */

    @Override
    public final boolean attackEntityFrom(DamageSource damagesource, float amount) {
        cam72cam.mod.entity.Entity wrapEnt = new cam72cam.mod.entity.Entity(damagesource.getTrueSource());
        DamageType type;
        if (damagesource.isExplosion() && !(damagesource.getTrueSource() instanceof MobEntity)) {
            type = DamageType.EXPLOSION;
        } else if (damagesource.getTrueSource() instanceof PlayerEntity) {
            type = damagesource.isProjectile() ? DamageType.PROJECTILE : DamageType.PLAYER;
        } else {
            type = DamageType.OTHER;
        }
        iKillable.onDamage(type, wrapEnt, amount);

        return false;
    }

    @Override
    protected void registerData() {

    }

    @Override
    public final void remove() {
        if (this.isAlive()) {
            super.remove();
            iKillable.onRemoved();
        }
    }

    /* Ridable */

    @Override
    public boolean canFitPassenger(Entity passenger) {
        return iRidable.canFitPassenger(new cam72cam.mod.entity.Entity(passenger));
    }

    private Vec3d calculatePassengerOffset(cam72cam.mod.entity.Entity passenger) {
        return passenger.getPosition().subtract(self.getPosition()).rotateMinecraftYaw(-self.getRotationYaw());
    }

    private Vec3d calculatePassengerPosition(Vec3d offset) {
        return offset.rotateMinecraftYaw(-self.getRotationYaw()).add(self.getPosition());
    }

    @Override
    public final void addPassenger(Entity entity) {
        if (!world.isRemote) {
            ModCore.debug("New Seat");
            SeatEntity seat = new SeatEntity(SeatEntity.TYPE, world);
            seat.setup(this, entity);
            cam72cam.mod.entity.Entity passenger = self.getWorld().getEntity(entity);
            passengerPositions.put(entity.getUniqueID(), iRidable.getMountOffset(passenger, calculatePassengerOffset(passenger)));
            entity.startRiding(seat);
            //updateSeat(seat); Don't do this here, can cause StackOverflow
            updateSeat(seat);
            world.addEntity(seat);
            self.sendToObserving(new PassengerPositionsPacket(this));
        } else {
            ModCore.debug("skip");
        }
    }

    List<cam72cam.mod.entity.Entity> getActualPassengers() {
        return seats.stream()
                .map(SeatEntity::getEntityPassenger)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    void updateSeat(SeatEntity seat) {
        if (!seats.contains(seat)) {
            seats.add(seat);
        }

        cam72cam.mod.entity.Entity passenger = seat.getEntityPassenger();
        if (passenger != null) {
            Vec3d offset = passengerPositions.get(passenger.getUUID());
            // Weird case around player joining with a different UUID during debugging
            if (offset == null) {
                offset = iRidable.getMountOffset(passenger, calculatePassengerOffset(passenger));
                passengerPositions.put(passenger.getUUID(), offset);
            }

            offset = iRidable.onPassengerUpdate(passenger, offset);
            if (!seat.isPassenger(passenger.internal)) {
                return;
            }

            passengerPositions.put(passenger.getUUID(), offset);

            Vec3d pos = calculatePassengerPosition(offset);

            //TODO 1.14.4 if (world.loadedEntityList.indexOf(seat) < world.loadedEntityList.indexOf(passenger.internal)) {
                pos = pos.add(new Vec3d(getMotion()));
            //}*/

            passenger.setPosition(pos);
            passenger.setVelocity(new Vec3d(getMotion()));

            float delta = rotationYaw - prevRotationYaw;
            passenger.internal.rotationYaw = passenger.internal.rotationYaw + delta;

            seat.shouldSit = iRidable.shouldRiderSit(passenger);
        }
    }

    boolean isPassenger(cam72cam.mod.entity.Entity passenger) {
        return getActualPassengers().stream().anyMatch(p -> p.getUUID().equals(passenger.getUUID()));
    }

    void removeSeat(SeatEntity seat) {
        cam72cam.mod.entity.Entity passenger = seat.getEntityPassenger();
        if (passenger != null) {
            Vec3d offset = passengerPositions.get(passenger.getUUID());
            if (offset != null) {
                offset = iRidable.onDismountPassenger(passenger, offset);
                passenger.setPosition(calculatePassengerPosition(offset));
            }
            passengerPositions.remove(passenger.getUUID());
        }
        seats.remove(seat);
    }

    void removePassenger(cam72cam.mod.entity.Entity passenger) {
        for (SeatEntity seat : this.seats) {
            cam72cam.mod.entity.Entity seatPass = seat.getEntityPassenger();
            if (seatPass != null && seatPass.getUUID().equals(passenger.getUUID())) {
                passenger.internal.stopRiding();
                break;
            }
        }
    }

    @Override
    public boolean canRiderInteract() {
        return false;
    }

    public int getPassengerCount() {
        return seats.size();
    }

    /* ICollision */
    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return new BoundingBox(iCollision.getCollision());
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return new BoundingBox(iCollision.getCollision());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        AxisAlignedBB bb = this.getBoundingBox();
        return new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    /* Hacks */
    @Override
    public boolean canBeCollidedWith() {
        // Needed for right click, probably a forge or MC bug
        return true;
    }

    @Override
    public boolean canBePushed() {
        return settings.canBePushed;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        if (settings.defaultMovement) {
            super.setPositionAndRotationDirect(x, y, z, yaw, pitch, posRotationIncrements, teleport);
        }
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        if (settings.defaultMovement) {
            super.setVelocity(x, y, z);
        }
    }

    /*
     * Disable standard entity sync
     */


    private static class PassengerMapper implements TagMapper<Map<UUID, Vec3d>> {
        @Override
        public TagAccessor<Map<UUID, Vec3d>> apply(Class<Map<UUID, Vec3d>> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                (d, o) -> d.setMap(fieldName, o, UUID::toString, (Vec3d pos) -> new TagCompound().setVec3d("pos", pos)),
                d -> d.getMap(fieldName, UUID::fromString, t -> t.getVec3d("pos"))
            );
        }
    }

    public static class PassengerPositionsPacket extends Packet {
        @TagField
        private cam72cam.mod.entity.Entity target;

        @TagField(mapper = PassengerMapper.class)
        private Map<UUID, Vec3d> passengerPositions = new HashMap<>();

        public PassengerPositionsPacket() {}

        public PassengerPositionsPacket(ModdedEntity target) {
            this.target = target.self;
            this.passengerPositions = target.passengerPositions;
        }

        @Override
        public void handle() {
            if (target != null && target.internal instanceof ModdedEntity) {
                ModdedEntity target = (ModdedEntity) this.target.internal;
                target.passengerPositions = this.passengerPositions;
            }
        }
    }

    /*
     * TODO!!!
     */
    /*
    //@Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return false;//super.hasCapability(energyCapability, facing);
    }

    @SuppressWarnings("unchecked")
	//@Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) cargoItems;
        }
        return null;//super.getCapability(energyCapability, facing);
    }

	@Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
	@Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) theTank;
        }
        return super.getCapability(capability, facing);
    }
     */
}
