package cam72cam.mod.entity;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

/** Seat construct to make multiple riders actually work */
public class SeatEntity extends Entity implements IEntityAdditionalSpawnData {
    static final ResourceLocation ID = new ResourceLocation(ModCore.MODID, "seat");
    public static final EntityType<SeatEntity> TYPE = makeType();

    private static EntityType<SeatEntity> makeType() {
        EntityType.IFactory<SeatEntity> ctr = SeatEntity::new;
        EntityType<SeatEntity> et = EntityType.Builder.of(ctr, EntityClassification.MISC)
                .setShouldReceiveVelocityUpdates(false)
                .setTrackingRange(512)
                .setUpdateInterval(20)
                .fireImmune()
                .setCustomClientFactory((msg, world) -> new SeatEntity(Registry.ENTITY_TYPE.byId(msg.getTypeId()), world))
                .build(SeatEntity.ID.toString());
        et.setRegistryName(ID);
        return et;
    }

    static {
        World.onTick(SeatEntity::ticker);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> ClientEvents.TICK.subscribe(() -> {
            if (MinecraftClient.isReady()) {
                ticker(MinecraftClient.getPlayer().getWorld());
            }
        }));
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

    public SeatEntity(EntityType type, net.minecraft.world.World worldIn) {
        super(type, worldIn);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT compound) {
        TagCompound data = new TagCompound(compound);
        parent = data.getUUID("parent");
        passenger = data.getUUID("passenger");
        shouldSit = data.getBoolean("shouldSit");
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT compound) {
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
            this.remove();
            return;
        }
        if (passenger == null) {
            ModCore.debug("No passenger, goodbye");
            this.remove();
            return;
        }

        if (getPassengers().isEmpty()) {
            if (this.ticks < 20) {
                if (!hasHadPassenger) {
                    cam72cam.mod.entity.Entity toRide = World.get(level).getEntity(passenger, cam72cam.mod.entity.Entity.class);
                    if (toRide != null) {
                        ModCore.debug("FORCE RIDER");
                        toRide.internal.startRiding(this, true);
                        hasHadPassenger = true;
                    }
                }
            } else {
                ModCore.debug("No passengers, goodbye");
                this.remove();
                return;
            }
        }

        if (getParent() == null) {
            if (ticks > 20) {
                ModCore.debug("No parent found, goodbye");
                this.remove();
            }
        }
    }

    public void setup(ModdedEntity moddedEntity, Entity passenger) {
        this.parent = moddedEntity.getUUID();
        this.setPos(moddedEntity.getX(), moddedEntity.getY(), moddedEntity.getZ());
        this.passenger = passenger.getUUID();
    }

    public void moveTo(ModdedEntity moddedEntity) {
        this.parent = moddedEntity.getUUID();
    }

    public cam72cam.mod.entity.Entity getParent() {
        cam72cam.mod.entity.Entity linked = World.get(level).getEntity(parent, cam72cam.mod.entity.Entity.class);
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
    public final void updatePassengerPreTick(net.minecraft.entity.Entity passenger) {
        if (lastUpdateTick != this.ticks) {
            lastUpdateTick = this.ticks;
            cam72cam.mod.entity.Entity linked = World.get(level).getEntity(parent, cam72cam.mod.entity.Entity.class);
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
    public final void removePassenger(net.minecraft.entity.Entity passenger) {
        cam72cam.mod.entity.Entity linked = World.get(level).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ((ModdedEntity) linked.internal).removeSeat(this);
        }
        super.removePassenger(passenger);
    }


    @Override
    public Vector3d getDismountLocationForPassenger(LivingEntity livingEntity) {
        return livingEntity.position();
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public cam72cam.mod.entity.Entity getEntityPassenger() {
        if (!this.isAlive()) {
            return null;
        }
        if (this.getPassengers().size() == 0) {
            return null;
        }
        return World.get(level).getEntity(getPassengers().get(0));
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        TagCompound data = new TagCompound();
        data.setUUID("parent", parent);
        data.setUUID("passenger", passenger);
        buffer.writeNbt(data.internal);
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        TagCompound data = new TagCompound(additionalData.readNbt());
        parent = data.getUUID("parent");
        passenger = data.getUUID("passenger");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;
    }
}
