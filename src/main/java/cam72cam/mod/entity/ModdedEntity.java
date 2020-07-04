package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.custom.*;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.*;
import cam72cam.mod.util.Hand;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.*;
import java.util.stream.Collectors;

public class ModdedEntity extends Entity implements IEntityAdditionalSpawnData {
    private cam72cam.mod.entity.Entity self;

    @TagField(value = "passengers", mapper = PassengerMapper.class)
    private Map<UUID, Vec3d> passengerPositions = new HashMap<>();

    private List<SeatEntity> seats = new ArrayList<>();

    private EntitySettings settings;
    private String type;
    private IWorldData iWorldData;
    private ITickable iTickable;
    private IClickable iClickable;
    private IKillable iKillable;
    private IRidable iRidable;
    private ICollision iCollision;

    public ModdedEntity(World world) {
        super(world);

        super.preventEntitySpawning = true;
    }

    @Override
    protected final void entityInit() {
    }

    /* Init Self Wrapper */

    protected final void init(String type) {
        if (self == null) {
            this.type = type;
            self = EntityRegistry.create(type, this);

            EntitySettings settings = EntityRegistry.getSettings(type);
            super.isImmuneToFire = settings.immuneToFire;
            super.entityCollisionReduction = settings.entityCollisionReduction;
            this.settings = settings;

            iWorldData = IWorldData.get(self);
            iTickable = ITickable.get(self);
            iClickable = IClickable.get(self);
            iKillable = IKillable.get(self);
            iRidable = IRidable.get(self);
            iCollision = ICollision.get(self);
        }
    }

    private final void loadSelf(TagCompound data) {
        String type = data.getString("custom_mob_type");
        if (type == null) {
            // Legacy...
            type = data.getString("id");
        }
        if (type == null) {
            throw new RuntimeException("Invalid entity data: " + data);
        }
        init(type);
    }

    private final void saveSelf(TagCompound data) {
        data.setString("custom_mob_type", type);
    }

    public cam72cam.mod.entity.Entity getSelf() {
        return self;
    }

    /* IWorldData */

    @Override
    protected final void readEntityFromNBT(NBTTagCompound compound) {
        load(new TagCompound(compound));
    }

    private final void load(TagCompound data) {
        loadSelf(data);
        try {
            TagSerializer.deserialize(data, this);
            TagSerializer.deserialize(data, self);
        } catch (SerializationException e) {
            ModCore.catching(e, "Error during entity load: %s - %s", self, data);
        }
        iWorldData.load(data);
    }

    @Override
    protected final void writeEntityToNBT(NBTTagCompound compound) {
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
        saveSelf(data);
    }

    /* ISpawnData */

    @Override
    public final void readSpawnData(ByteBuf additionalData) {
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
        data.set("sync", self.sync);
        save(data);
        ByteBufUtils.writeTag(buffer, data.internal);
    }

    /* ITickable */

    @Override
    public void onEntityUpdate() {
        iTickable.onTick();
        try {
            self.sync.send();
        } catch (SerializationException e) {
            ModCore.catching(e, "Unable to send sync data for %s - %s", this, self.sync);
        }

        //TODO 1.7.10
        super.boundingBox.setBB(new BoundingBox(iCollision.getCollision()));

        if (this.riddenByEntity != null) {
            addPassenger(this.riddenByEntity);
        }

        if (!seats.isEmpty()) {
            seats.removeAll(seats.stream().filter(x -> x.isDead).collect(Collectors.toList()));
            seats.forEach(seat -> seat.setPosition(posX, posY, posZ));
        }
    }

    /* Player Interact */

    @Override
    public final boolean interactFirst(EntityPlayer player) {
        return iClickable.onClick(new Player(player), Hand.PRIMARY) == ClickResult.ACCEPTED;
    }

    /* Death */

    @Override
    public final boolean attackEntityFrom(DamageSource damagesource, float amount) {
        cam72cam.mod.entity.Entity wrapEnt = new cam72cam.mod.entity.Entity(damagesource.getSourceOfDamage());
        DamageType type;
        if (damagesource.isExplosion() && !(damagesource.getSourceOfDamage() instanceof EntityMob)) {
            type = DamageType.EXPLOSION;
        } else if (damagesource.getSourceOfDamage() instanceof EntityPlayer) {
            type = damagesource.isProjectile() ? DamageType.PROJECTILE : DamageType.PLAYER;
        } else {
            type = DamageType.OTHER;
        }
        iKillable.onDamage(type, wrapEnt, amount);

        return false;
    }

    @Override
    public final void setDead() {
        if (!this.isDead) {
            super.setDead();
            iKillable.onRemoved();
        }
    }

    /* Ridable */

    /* TODO 1.7.10
    @Override
    public boolean canFitPassenger(Entity passenger) {
        return iRidable.canFitPassenger(new cam72cam.mod.entity.Entity(passenger));
    }
    */

    private Vec3d calculatePassengerOffset(cam72cam.mod.entity.Entity passenger) {
        return passenger.getPosition().subtract(self.getPosition()).rotateMinecraftYaw(-self.getRotationYaw());
    }

    private Vec3d calculatePassengerPosition(Vec3d offset) {
        return offset.rotateMinecraftYaw(-self.getRotationYaw()).add(self.getPosition());
    }

    public final void addPassenger(Entity entity) {
        if (!worldObj.isRemote) {
            entity.mountEntity(null);
            ModCore.debug("New Seat");
            SeatEntity seat = new SeatEntity(this.worldObj);
            seat.setup(this, entity);
            cam72cam.mod.entity.Entity passenger = self.getWorld().getEntity(entity);
            passengerPositions.put(entity.getPersistentID(), iRidable.getMountOffset(passenger, calculatePassengerOffset(passenger)));
            seat.setPosition(posX, posY, posZ);
            entity.mountEntity(seat);
            //updateSeat(seat); Don't do this here, can cause StackOverflow
            worldObj.spawnEntityInWorld(seat);
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
            if (seat.riddenByEntity != passenger.internal) {
                return;
            }

            passengerPositions.put(passenger.getUUID(), offset);

            Vec3d pos = calculatePassengerPosition(offset);
            if (passenger.internal instanceof EntityPlayer) {
                pos = pos.add(0, 1.5, 0);
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

    boolean isPassenger(cam72cam.mod.entity.Entity passenger) {
        return getActualPassengers().stream().anyMatch(p -> p.getUUID().equals(passenger.getUUID()));
    }

    void removeSeat(SeatEntity seat) {
        cam72cam.mod.entity.Entity passenger = seat.getEntityPassenger();
        if (passenger != null) {
            Vec3d offset = passengerPositions.get(passenger.getUUID());
            if (offset != null) {
                offset = iRidable.onDismountPassenger(passenger, offset);
                if (passenger instanceof Player) {
                    offset.add(0, 1.5, 0);
                }
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
                passenger.internal.mountEntity(null);
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
    public AxisAlignedBB getCollisionBox(Entity collider) {
        return null;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return new BoundingBox(iCollision.getCollision());
    }

    /* TODO 1.7.10
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        AxisAlignedBB bb = this.getEntityBoundingBox();
        return new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }
    */

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

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int posRotationIncrements) {
        if (settings.defaultMovement) {
            super.setPositionAndRotation2(x, y, z, yaw, pitch, posRotationIncrements);
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

    public String getName() {
        return this.type;
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
