package cam72cam.mod.entity;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.world.World;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import java.util.List;
import java.util.UUID;

/** Seat construct to make multiple riders actually work */
public class SeatEntity extends Entity implements IEntityWithComplexSpawn {
    static final ResourceLocation ID = ResourceLocation.tryBuild(ModCore.MODID, "seat");
    public static final EntityType<SeatEntity> TYPE = makeType();

    private static EntityType<SeatEntity> makeType() {
        EntityType.EntityFactory<SeatEntity> ctr = SeatEntity::new;
        EntityType<SeatEntity> et = EntityType.Builder.of(ctr, MobCategory.MISC)
                .setShouldReceiveVelocityUpdates(false)
                .setTrackingRange(512)
                .setUpdateInterval(20)
                .fireImmune()
//                .setCustomClientFactory((msg, world) -> new SeatEntity(BuiltInRegistries.ENTITY_TYPE.byId(msg.getTypeId()), world))
                .build(SeatEntity.ID.toString());
        return et;
    }

    static {
        World.onTick(SeatEntity::ticker);
        if (FMLEnvironment.dist.isClient()) {
            ClientEvents.TICK_POST.subscribe(() -> {
                if (MinecraftClient.isReady()) {
                    ticker(MinecraftClient.getPlayer().getWorld());
                }
            });
        }
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

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
    public Vec3 getPassengerRidingPosition(Entity entity) {
        Vec3 vec3 = super.getPassengerRidingPosition(entity);
        return new Vec3(vec3.x, 0, vec3.z);
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

//    @Override
//    public Packet<ClientGamePacketListener> getAddEntityPacket() {
//        return NetworkHooks.getEntitySpawningPacket(this);
//    }

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
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        TagCompound data = new TagCompound();
        data.setUUID("parent", parent);
        data.setUUID("passenger", passenger);
        buffer.writeNbt(data.internal);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        TagCompound data = new TagCompound(additionalData.readNbt());
        parent = data.getUUID("parent");
        passenger = data.getUUID("passenger");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;
    }
}
