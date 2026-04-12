package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Light {
    private static final ConcurrentLinkedDeque<LightEntity> lights = new ConcurrentLinkedDeque<>();

    private static final EntityType<LightEntity>[] types = new EntityType[16];

    private LightEntity internal;
    private double lightLevel;

    public Light(World world, Vec3d pos, double lightLevel) {
        init(world.internal, pos.internal(), lightLevel);
    }

    public void remove() {
        internal.remove(Entity.RemovalReason.KILLED);
        internal = null;
    }

    public void setPosition(Vec3d pos) {
        internal.setPos(pos.x, pos.y, pos.z);
    }

    public void setLightLevel(double lightLevel) {
        init(internal.level(), internal.position(), lightLevel);
    }

    private void init(Level world, Vec3 pos, double lightLevel) {
        if (lightLevel == this.lightLevel) {
            // NOP
            return;
        }
        if (internal != null) {
            internal.remove(Entity.RemovalReason.KILLED);
        }
        int ll = (int) Math.ceil((lightLevel * 15));
        ll = Math.min(ll, 15);
        ll = Math.max(ll, 1);
        EntityType<LightEntity> type = types[ll];
        internal = type.create(world);
        internal.setPos(pos.x, pos.y, pos.z);
//        world.addFreshEntity(internal);
        this.lightLevel = lightLevel;
        lights.add(internal);
    }

    public static void register() {
        CommonEvents.Entity.REGISTER.subscribe(helper -> {
            for (int i = 1; i <= 15; i++) {
                EntityType.Builder<LightEntity> builder = EntityType.Builder.of(LightEntity::new, MobCategory.MISC);
                builder.fireImmune();
                builder.sized(0, 0);

                EntityType<LightEntity> et = builder.build("light" + i);
                helper.register(Objects.requireNonNull(ResourceLocation.tryParse("universalmodcore:light" + i)), et);
                types[i] = et;
            }
        });
    }

    public static void registerClient() {
        if(isLDLInstalled()) {
            ClientEvents.TICK.subscribe(Light::onClientTick);
        }
    }

    public static void onClientTick() {
        if(MinecraftClient.isPaused()) return;
        for (LightEntity light : lights) {
            ClientLevel level = (ClientLevel) light.level();
            level.guardEntityTick(level::tickNonPassenger, light);
        }
    }

    // Client only
    private static class LightEntity extends Entity {
        public LightEntity(EntityType<?> entityTypeIn, Level world) {
            super(entityTypeIn, world);
            super.noPhysics = true;
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {

        }

        @Override
        protected void readAdditionalSaveData(CompoundTag p_20052_) {

        }

        @Override
        protected void addAdditionalSaveData(CompoundTag p_20139_) {

        }
    }

    public static boolean enabled() {
        boolean flag = isLDLInstalled();
        if (flag) {
            try {
                //TODO Optimize me!
                Class<?> ldl = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
                Method getInstance = ldl.getDeclaredMethod("get");
                Object inst = getInstance.invoke(null);
                Field field = ldl.getDeclaredField("config");
                field.setAccessible(true);
                Object cfg = field.get(inst);
                Class<?> config = Class.forName("dev.lambdaurora.lambdynlights.DynamicLightsConfig");
                Method entities = config.getDeclaredMethod("getEntitiesLightSource");
                Class<?> setting = Class.forName("dev.lambdaurora.lambdynlights.config.SettingEntry");
                Method get = setting.getDeclaredMethod("get");
                return (Boolean) get.invoke(setting.cast(entities.invoke(cfg)));
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                     IllegalAccessException | NoSuchFieldException e) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLDLInstalled() {
        try {
            Class<?> cls = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static class UMCDynLightInitializer implements DynamicLightsInitializer {
        @Override
        public void onInitializeDynamicLights(DynamicLightsContext lightCtx) {
            lightCtx.entityLightSourceManager().onRegisterEvent().register(context -> {
                for (int i = 1; i <= 15; i++) {
                    EntityType<LightEntity> et = types[i];
                    context.register(et, i);
                }
            });
        }

        @Deprecated(forRemoval = true)
        @Override
        public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager) {
            //Deprecated
        }
    }
}
