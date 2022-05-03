package cam72cam.mod.render;

import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class Light {
    private static EntityType<LightEntity>[] types = new EntityType[15];

    private LightEntity internal;
    private double lightLevel;

    public Light(World world, Vec3d pos, double lightLevel) {
        init(world.internal, pos.internal(), lightLevel);
    }

    public void remove() {
        internal.remove();
        internal = null;
    }

    public void setPosition(Vec3d pos) {
        internal.setPosition(pos.x, pos.y, pos.z);
    }

    public void setLightLevel(double lightLevel) {
        init(internal.world, internal.getPositionVector(), lightLevel);
    }

    private void init(net.minecraft.world.World world, net.minecraft.util.math.Vec3d pos, double lightLevel) {
        if (lightLevel == this.lightLevel) {
            // NOP
            return;
        }
        if (internal != null) {
            internal.remove();
        }
        int ll = (int) Math.ceil((lightLevel * 15));
        ll = Math.min(ll, 15);
        ll = Math.max(ll, 1);
        EntityType<LightEntity> type = types[ll];
        internal = type.create(world);
        internal.setPosition(pos.x, pos.y, pos.z);
        world.addEntity(internal);
        this.lightLevel = lightLevel;
    }

    public static void register() {
        CommonEvents.Entity.REGISTER.subscribe(() -> {
            for (int i = 1; i <= 15; i++) {
                EntityType.Builder<LightEntity> builder = EntityType.Builder.create(LightEntity::new, EntityClassification.MISC);
                builder.immuneToFire();
                builder.size(0, 0);

                EntityType<LightEntity> et = builder.build("light" + i);
                et.setRegistryName(new ResourceLocation("universalmodcore:light" + i));
                ForgeRegistries.ENTITIES.register(et);
                types[i] = et;
            }
        });
    }

    // Client only
    private static class LightEntity extends Entity {
        public LightEntity(EntityType<?> entityTypeIn, net.minecraft.world.World world) {
            super(entityTypeIn, world);
            super.noClip = true;
        }

        @Override
        protected void registerData() {

        }

        @Override
        public void tick() {
            this.prevPosX = this.getPosX();
            this.prevPosY = this.getPosY();
            this.prevPosZ = this.getPosZ();
        }

        @Override
        protected void readAdditional(CompoundNBT compound) {

        }

        @Override
        protected void writeAdditional(CompoundNBT compound) {

        }

        @Override
        public IPacket<?> createSpawnPacket() {
            return null;
        }
    }

    public static boolean enabled() {
        if (!OptiFine.isLoaded()) {
            return false;
        }
        try {
            Class<?> optiConfig = Class.forName("Config");
            return Objects.equals(true, optiConfig.getDeclaredMethod("isDynamicLights").invoke(null));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }
}
