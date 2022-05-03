package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.entity.custom.*;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import cam72cam.mod.util.SingleCache;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Internal class which extends MC's Entity.  Do not use directly */
public class ModdedEntity extends Entity implements IEntityAdditionalSpawnData {
    // Reference to the entity that this is representing
    private CustomEntity self;

    // Keeps track of where passengers are within this entity
    @TagField(value = "passengers", mapper = PassengerMapper.class)
    private Map<UUID, Vec3d> passengerPositions = new HashMap<>();

    // All of the known seats attached to this entity
    private final List<SeatEntity> seats = new ArrayList<>();

    // Views of self that implement different interfaces
    private IWorldData iWorldData;
    private ITickable iTickable;
    private IClickable iClickable;
    private IKillable iKillable;
    private IRidable iRidable;
    private ICollision iCollision;
    private String legacyId;

    Pair<String, TagCompound> refusedToJoin = null;

    /** Standard forge constructor */
    public ModdedEntity(EntityType type, World world, Supplier<CustomEntity> ctr) {
        super(type, world);

        super.blocksBuilding = true;

        self = ctr.get();
        self.setup(this);

        iWorldData = IWorldData.get(self);
        iTickable = ITickable.get(self);
        iClickable = IClickable.get(self);
        iKillable = IKillable.get(self);
        iRidable = IRidable.get(self);
        iCollision = ICollision.get(self);
    }

    private final SingleCache<IBoundingBox, AxisAlignedBB> cachedCollisionBB = new SingleCache<>(BoundingBox::from);
    private final SingleCache<IBoundingBox, AxisAlignedBB> cachedRenderBB = new SingleCache<>(internal -> {
        AxisAlignedBB bb = BoundingBox.from(internal);
        /*
         So why do we wrap this with a new AABB here instead of passing the BB straight through?
         Good question
         Certain mods (like IR) use custom bounding boxes that do some really funky shit to break
         the axis constraint.  We don't care about that when rendering, just want a worst-case sized BB
        */
        return new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    });

    public CustomEntity getSelf() {
        return self;
    }

    /* IWorldData */

    /** @see #load */
    @Override
    public final void readAdditionalSaveData(CompoundNBT compound) {
        load(new TagCompound(compound));
    }

    /**
     * Deserializes data into this, self, and calls self.load.
     * @see IWorldData
     */
    private final void load(TagCompound data) {
        // Deserialize data into this using annotations
        try {
            TagSerializer.deserialize(data, this);
        } catch (SerializationException e) {
            ModCore.catching(e, "Error during entity load: %s - %s", this, data);
        }

        TagCompound selfData = data.get("selfData");
        if (selfData == null) {
            // Old style used to save everything in one giant NBT blob.  New versions save self in a sub tag.
            selfData = data;
        }

        if (data.hasKey("custom_mob_type")) {
            legacyId = data.getString("custom_mob_type");
            self = EntityRegistry.create(legacyId, this);
            self.setup(this);

            iWorldData = IWorldData.get(self);
            iTickable = ITickable.get(self);
            iClickable = IClickable.get(self);
            iKillable = IKillable.get(self);
            iRidable = IRidable.get(self);
            iCollision = ICollision.get(self);

            data.setString("id", legacyId);
        }

        // Deserialize self
        try {
            TagSerializer.deserialize(selfData, self);
        } catch (SerializationException e) {
            ModCore.catching(e, "Error during entity load: %s - %s", self, selfData);
        }
        String error = self.tryJoinWorld();
        if (error != null) {
            refusedToJoin = Pair.of(error, selfData);
        } else {
            iWorldData.load(selfData);
        }
    }

    /** @see #save */
    @Override
    public void addAdditionalSaveData(CompoundNBT compound) {
        save(new TagCompound(compound));
    }

    /**
     * Inverse of load
     * @see IWorldData
     * @see #load
     */
    private void save(TagCompound data) {
        try {
            TagSerializer.serialize(data, this);
        } catch (SerializationException e) {
            ModCore.catching(e, "Error during entity save: %s", this);
        }
        iWorldData.save(data);

        TagCompound selfData;
        if (refusedToJoin == null) {
            selfData = new TagCompound();
            try {
                TagSerializer.serialize(selfData, self);
            } catch (SerializationException e) {
                ModCore.catching(e, "Error during entity save: %s", self);
            }
            iWorldData.save(selfData);
        } else {
            selfData = refusedToJoin.getValue();
        }

        data.set("selfData", selfData);
    }

    /* ISpawnData */

    /** @see #load */
    @Override
    public final void readSpawnData(PacketBuffer additionalData) {
        TagCompound data = new TagCompound(additionalData.readNbt());
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
        buffer.writeNbt(data.internal);
    }

    @Override
    public EntityType<?> getType() {
        return legacyId == null ? super.getType() : ForgeRegistries.ENTITIES.getValue(new ResourceLocation(legacyId));
    }

    /* ITickable */

    /**
     * Hooks into #ITickable and runs server/client data synchronization.  Also does some seat management.
     *
     * @see ITickable
     */
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
            seats.forEach(seat -> seat.setPos(getX(), getY(), getZ()));
        }
    }

    /* Player Interact */
    /** @see IClickable */
    @Override
    public final ActionResultType interact(PlayerEntity player, net.minecraft.util.Hand hand) {
        return iClickable.onClick(new Player(player), Player.Hand.from(hand)).internal;
    }

    /* Death */

    /** @see IKillable */
    @Override
    public final boolean hurt(DamageSource damagesource, float amount) {
        cam72cam.mod.entity.Entity wrapEnt = damagesource.getDirectEntity() != null ? self.getWorld().getEntity(damagesource.getDirectEntity()) : null;
        DamageType type;
        if (damagesource.isExplosion()) {
            type = DamageType.EXPLOSION;
        } else if (damagesource.isProjectile()) {
            type = DamageType.PROJECTILE;
        } else if (damagesource.isFire()) {
            type = DamageType.FIRE;
        } else if (damagesource.isMagic()) {
            type = DamageType.MAGIC;
        } else {
            type = DamageType.OTHER;
        }
        iKillable.onDamage(type, wrapEnt, amount, damagesource.isBypassInvul());

        return false;
    }

    /** @see IKillable */
    @Override
    protected void defineSynchedData() {

    }

    @Override
    public final void remove() {
        if (this.isAlive()) {
            super.remove();
            iKillable.onRemoved();
        }
    }

    /* Ridable */
    /** @see IRidable#canFitPassenger */
    @Override
    public boolean canAddPassenger(Entity passenger) {
        return iRidable.canFitPassenger(self.getWorld().getEntity(passenger));
    }

    /** Passenger offset from entity center rotated by entity yaw */
    private Vec3d calculatePassengerOffset(cam72cam.mod.entity.Entity passenger) {
        return passenger.getPosition().subtract(self.getPosition()).rotateYaw(self.getRotationYaw());
    }

    /** Rotate offset around entity center by entity yaw and add entity center */
    private Vec3d calculatePassengerPosition(Vec3d offset) {
        return offset.rotateYaw(-self.getRotationYaw()).add(self.getPosition());
    }

    /**
     * Don't actually add passengers to the Entity, add a seat which follows the entity instead.
     *
     * Only functions on the server.
     *
     * @see IRidable#getMountOffset
     */
    @Override
    public final void addPassenger(Entity entity) {
        if (!level.isClientSide) {
            SeatEntity seat = new SeatEntity(SeatEntity.TYPE, level);
            seat.setup(this, entity);
            cam72cam.mod.entity.Entity passenger = self.getWorld().getEntity(entity);
            passengerPositions.put(entity.getUUID(), iRidable.getMountOffset(passenger, calculatePassengerOffset(passenger)));
            entity.startRiding(seat);
            //updateSeat(seat); Don't do this here, can cause StackOverflow
            level.addFreshEntity(seat);
            new PassengerPositionsPacket(this).sendToObserving(self);
        }
    }

    /**
     * Returns passengers that are riding via seats
     * @see CustomEntity#getPassengers
     */
    List<cam72cam.mod.entity.Entity> getActualPassengers() {
        return seats.stream()
                .map(SeatEntity::getEntityPassenger)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Helper function that updates a seat's position and it's rider's position
     */
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
            if (!seat.hasPassenger(passenger.internal)) {
                return;
            }

            passengerPositions.put(passenger.getUUID(), offset);

            Vec3d pos = calculatePassengerPosition(offset);

            passenger.setPosition(pos);
            passenger.setVelocity(new Vec3d(getDeltaMovement()));

            float delta = yRot - yRotO;
            passenger.internal.yRot = passenger.internal.yRot + delta;

            seat.shouldSit = iRidable.shouldRiderSit(passenger);
        }
    }

    /** @see CustomEntity#isPassenger */
    boolean isPassenger(cam72cam.mod.entity.Entity passenger) {
        return getActualPassengers().stream().anyMatch(p -> p.getUUID().equals(passenger.getUUID()));
    }

    public void moveRiderTo(cam72cam.mod.entity.Entity entity, CustomEntity other) {
        if (other.internal.iRidable.canFitPassenger(entity)) {
            SeatEntity seat = (SeatEntity) entity.internal.getVehicle();
            this.seats.remove(seat);
            seat.moveTo(other.internal);
            other.internal.seats.add(seat);
            other.internal.passengerPositions.remove(entity.getUUID());
            if (!level.isClientSide) {
                new PassengerSeatPacket(other, entity).sendToObserving(self);
            }
        }
    }

    /**
     * @see IRidable#onDismountPassenger
     * @see SeatEntity#removePassenger
     */
    void removeSeat(SeatEntity seat) {
        cam72cam.mod.entity.Entity passenger = seat.getEntityPassenger();
        if (passenger != null) {
            Vec3d offset = passengerPositions.get(passenger.getUUID());
            if (offset != null) {
                offset = iRidable.onDismountPassenger(passenger, offset);

                Vec3d pos = calculatePassengerPosition(offset);

                while (!(level.isEmptyBlock(new Vec3i(pos).internal()) && level.isEmptyBlock(new Vec3i(pos).up().internal()))) {
                    pos = pos.add(0, 1, 0);
                }
                passenger.setPosition(pos);
            }
            passengerPositions.remove(passenger.getUUID());
        }
        seats.remove(seat);
    }

    /** @see CustomEntity#removePassenger */
    void removePassenger(cam72cam.mod.entity.Entity passenger) {
        for (SeatEntity seat : this.seats) {
            cam72cam.mod.entity.Entity seatPass = seat.getEntityPassenger();
            if (seatPass != null && seatPass.getUUID().equals(passenger.getUUID())) {
                passenger.internal.stopRiding();
                break;
            }
        }
    }

    // TODO
    @Override
    public boolean canRiderInteract() {
        return false;
    }

    public int getPassengerCount() {
        return seats.size();
    }

    /* ICollision NOTE: set width/height if implementing LivingEntity */
    /** @see #getEntityBoundingBox() */
    /* Removed 1.16
    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return getBoundingBox();
    }*/

    /**
     * Only generates a new BB object when the underlying self.getCollision() changes
     *
     * @see ICollision
     */
    @Override
    public AxisAlignedBB getBoundingBox() {
        if (refusedToJoin != null) {
            // Entity is added to a chunk but not world (yay minecraft)
            return super.getBoundingBox();
        }
        return cachedCollisionBB.get(iCollision.getCollision());
    }

    /**
     * Only generates a new BB object when the underlying self.getCollision() changes
     * TODO provide a way of specifying a render bounding box without a collision bounding box
     * @see ICollision
     */
    @Override
    public AxisAlignedBB getBoundingBoxForCulling() {
        return cachedRenderBB.get(iCollision.getCollision());
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    /* Hacks */
    /** Needed for right click, probably a forge or MC bug */
    @Override
    public boolean isPickable() {
        return true;
    }
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return self.canBePushed();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        if (self.allowsDefaultMovement()) {
            super.lerpTo(x, y, z, yaw, pitch, posRotationIncrements, teleport);
        }
    }

    @Override
    public void setDeltaMovement(double x, double y, double z) {
        if (self.allowsDefaultMovement()) {
            super.setDeltaMovement(x, y, z);
        }
    }

    /*
    @Override
    public String getName() {
        return this.type;
    }*/

    /*
    Passenger helpers
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

    public static class PassengerSeatPacket extends Packet {
        @TagField
        private CustomEntity target;
        @TagField
        private cam72cam.mod.entity.Entity rider;

        public PassengerSeatPacket() {}

        public PassengerSeatPacket(CustomEntity target, cam72cam.mod.entity.Entity rider) {
            this.target = target;
            this.rider = rider;
        }


        @Override
        protected void handle() {
            if (target != null) {
                target.addPassenger(rider);
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
