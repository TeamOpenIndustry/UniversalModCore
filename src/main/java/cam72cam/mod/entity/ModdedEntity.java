package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.entity.custom.*;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.*;
import cam72cam.mod.util.SingleCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
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

    // Registry name of self
    private String type;

    // Views of self that implement different interfaces
    private IWorldData iWorldData;
    private ITickable iTickable;
    private IClickable iClickable;
    private IKillable iKillable;
    private IRidable iRidable;
    private ICollision iCollision;

    private final SingleCache<IBoundingBox, AxisAlignedBB> cachedCollisionBB = new SingleCache<>(BoundingBox::from);
    private final SingleCache<IBoundingBox, AxisAlignedBB> cachedRenderBB = new SingleCache<>(internal -> {
        AxisAlignedBB bb = BoundingBox.from(internal);
        /*
         So why do we wrap this with a new AABB here instead of passing the BB straight through?
         Good question
         Certain mods (like IR) use custom bounding boxes that do some really funky shit to break
         the axis constraint.  We don't care about that when rendering, just want a worst-case sized BB
        */
        return AxisAlignedBB.getBoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    });

    Pair<String, TagCompound> refusedToJoin = null;

    /** Standard forge constructor */
    public ModdedEntity(World world) {
        super(world);

        super.preventEntitySpawning = true;
    }

    @Override
    protected final void entityInit() {
    }

    /** Setup self if we have not done so already.  This happens during entity data load. */
    protected final void initSelf(String type) {
        if (self == null) {
            this.type = type;
            self = EntityRegistry.create(type, this);

            super.isImmuneToFire = self.isImmuneToFire();
            super.entityCollisionReduction = self.getCollisionReduction();

            iWorldData = IWorldData.get(self);
            iTickable = ITickable.get(self);
            iClickable = IClickable.get(self);
            iKillable = IKillable.get(self);
            iRidable = IRidable.get(self);
            iCollision = ICollision.get(self);
        }
    }

    public CustomEntity getSelf() {
        return self;
    }

    /* IWorldData */

    /** @see #load */
    @Override
    protected final void readEntityFromNBT(NBTTagCompound compound) {
        load(new TagCompound(compound));
    }

    /**
     * Deserializes data into this, self, and calls self.load.
     * @see IWorldData
     */
    private void load(TagCompound data) {
        String type = data.getString("custom_mob_type");
        if (type == null) {
            // Legacy...
            type = data.getString("id");
        }
        if (type == null) {
            throw new RuntimeException("Invalid entity data: " + data);
        }
        // Setup self construct (if not done so already)
        initSelf(type);

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
    protected final void writeEntityToNBT(NBTTagCompound compound) {
        save(new TagCompound(compound));
    }

    /**
     * Inverse of load
     * @see IWorldData
     * @see #load
     */
    private void save(TagCompound data) {
        data.setString("custom_mob_type", type);
        try {
            TagSerializer.serialize(data, this);
        } catch (SerializationException e) {
            ModCore.catching(e, "Error during entity save: %s", this);
        }

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
    public final void readSpawnData(ByteBuf additionalData) {
        if (cam72cam.mod.world.World.get(worldObj) == null) {
            // This can happen during a sudden disconnect...
            return;
        }
        TagCompound data = new TagCompound(ByteBufUtils.readTag(additionalData));
        this.entityUniqueID = data.getUUID("UUIDSYNC");
        load(data);
        try {
            self.sync.receive(data.get("sync"));
        } catch (SerializationException e) {
            ModCore.catching(e, "Invalid sync payload %s for %s", data.get("sync"), this);
        }
    }

    @Override
    public final void writeSpawnData(ByteBuf buffer) {
        TagCompound data = new TagCompound();
        data.setUUID("UUIDSYNC", this.getPersistentID());
        TagCompound sync = new TagCompound();
        try {
            TagSerializer.serialize(sync, getSelf(), TagSync.class);
        } catch (Exception e) {
            ModCore.catching(e);
        }
        data.set("sync", sync);
        save(data);
        ByteBufUtils.writeTag(buffer, data.internal);
    }

    /* ITickable */

    /**
     * Hooks into #ITickable and runs server/client data synchronization.  Also does some seat management.
     *
     * @see ITickable
     */
    @Override
    public void onEntityUpdate() {
        iTickable.onTick();
        try {
            self.sync.send();
        } catch (SerializationException e) {
            ModCore.catching(e, "Unable to send sync data for %s - %s", this, self.sync);
        }

        //TODO 1.7.10
        super.boundingBox.setBB(BoundingBox.from(iCollision.getCollision()));

        if (this.riddenByEntity != null) {
            addPassenger(this.riddenByEntity);
        }

        if (!seats.isEmpty()) {
            seats.removeAll(seats.stream().filter(x -> x.isDead).collect(Collectors.toList()));
            seats.forEach(seat -> seat.setPosition(posX, posY, posZ));
        }

        if (this.ticksExisted % 20 == 0 && this.getPassengerCount() > 0 && !worldObj.isRemote) {
            new PassengerPositionsPacket(this).sendToObserving(self);
        }
    }

    /* Player Interact */
    /** @see IClickable */
    @Override
    public final boolean interactFirst(EntityPlayer player) {
        return iClickable.onClick(new Player(player), Player.Hand.PRIMARY).internal;
    }

    /* Death */

    /** @see IKillable */
    @Override
    public final boolean attackEntityFrom(DamageSource damagesource, float amount) {
        cam72cam.mod.entity.Entity wrapEnt = damagesource.getSourceOfDamage() != null ? self.getWorld().getEntity(damagesource.getSourceOfDamage()) : null;
        DamageType type;
        if (damagesource.isExplosion()) {
            type = DamageType.EXPLOSION;
        } else if (damagesource.isProjectile()) {
            type = DamageType.PROJECTILE;
        } else if (damagesource.isFireDamage()) {
            type = DamageType.FIRE;
        } else if (damagesource.isMagicDamage()) {
            type = DamageType.MAGIC;
        } else {
            type = DamageType.OTHER;
        }
        iKillable.onDamage(type, wrapEnt, amount, damagesource.isUnblockable());

        return false;
    }

    /** @see IKillable */
    @Override
    public final void setDead() {
        if (!this.isDead) {
            super.setDead();
            iKillable.onRemoved();
        }
    }

    /* Ridable */

    /** @see IRidable#canFitPassenger */
    /* TODO 1.7.10
    @Override
    public boolean canFitPassenger(Entity passenger) {
        return iRidable.canFitPassenger(self.getWorld().getEntity(passenger));
    }
    */

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
    // 1.7.10 @Override
    public final void addPassenger(Entity entity) {
        if (!worldObj.isRemote) {
            entity.mountEntity(null);
            ModCore.debug("New Seat");
            SeatEntity seat = new SeatEntity(worldObj);
            seat.setup(this, entity);
            cam72cam.mod.entity.Entity passenger = self.getWorld().getEntity(entity);
            passengerPositions.put(entity.getPersistentID(), iRidable.getMountOffset(passenger, calculatePassengerOffset(passenger)));
            seat.setPosition(posX, posY, posZ);
            entity.mountEntity(seat);
            //updateSeat(seat); Don't do this here, can cause StackOverflow
            worldObj.spawnEntityInWorld(seat);
            new PassengerPositionsPacket(this).sendToObserving(self);
        } else {
            ModCore.debug("skip");
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
     * 
     * @see SeatEntity#updatePassenger 
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
            if (seat.riddenByEntity != passenger.internal) {
                return;
            }

            passengerPositions.put(passenger.getUUID(), offset);

            Vec3d pos = calculatePassengerPosition(offset);
            if (passenger.internal instanceof EntityPlayer) {
                pos = pos.add(0, passenger.internal.yOffset, 0);
            }

            if (worldObj.loadedEntityList.indexOf(seat) < worldObj.loadedEntityList.indexOf(passenger.internal)) {
                pos = pos.add(motionX, motionY, motionZ);
            }

            passenger.setPosition(pos);
            passenger.setVelocity(new Vec3d(motionX, motionY, motionZ));

            float delta = rotationYaw - prevRotationYaw;
            passenger.internal.rotationYaw = passenger.internal.rotationYaw + delta;

            seat.shouldSit = iRidable.shouldRiderSit(passenger);
        }
    }

    /** @see CustomEntity#isPassenger */
    boolean isPassenger(cam72cam.mod.entity.Entity passenger) {
        return getActualPassengers().stream().anyMatch(p -> p.getUUID().equals(passenger.getUUID()));
    }

    public void moveRiderTo(cam72cam.mod.entity.Entity entity, CustomEntity other) {
        if (other.internal.iRidable.canFitPassenger(entity)) {
            SeatEntity seat = (SeatEntity) entity.internal.ridingEntity;
            this.seats.remove(seat);
            seat.moveTo(other.internal);
            other.internal.seats.add(seat);
            other.internal.passengerPositions.remove(entity.getUUID());
            if (!worldObj.isRemote) {
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

                Vec3d adjusted = pos;
                for (int i = 0; i < 6; i++) {
                    if (worldObj.isAirBlock((int) adjusted.x, (int) adjusted.y, (int) adjusted.z) && worldObj.isAirBlock((int) adjusted.x, (int) adjusted.y + 1, (int) adjusted.z)) {
                        pos = adjusted;
                        break;
                    }
                    adjusted = adjusted.add(0, 1, 0);
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
                passenger.internal.mountEntity(null);
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
    @Override
    public AxisAlignedBB getCollisionBox(Entity collider) {
        // TODO 1.7.10 getBoundingBox?
        return null;
    }

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
    /* 1.7.10
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return cachedRenderBB.get(iCollision.getCollision());
    }*/

    /* Hacks */
    /** Needed for right click, probably a forge or MC bug */
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canBePushed() {
        return self.canBePushed();
    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int posRotationIncrements) {
        if (self.allowsDefaultMovement()) {
            super.setPositionAndRotation2(x, y, z, yaw, pitch, posRotationIncrements);
        }
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        if (self.allowsDefaultMovement()) {
            super.setVelocity(x, y, z);
        }
    }

    /*
    // TODO 1.7.10 getEntityString?
    @Override
     */
    public String getName() {
        return this.type;
    }

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
            if (target != null && rider != null) {
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

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }
}
