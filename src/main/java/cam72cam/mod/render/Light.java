package cam72cam.mod.render;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Light {

    private static List<Runnable> toSpawn = new ArrayList<>();

    private static EntityType<LightEntity>[] types = new EntityType[16];

    private static boolean hasRegistered = false;

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
        // Add the entity next tick, this is probably in the middle of tick entity iteration and will break the iterator
        toSpawn.add(() -> ((ClientWorld) world).addEntity(internal.getEntityId(), internal));
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

        ClientEvents.REGISTER_ENTITY.subscribe(() -> {
            RenderingRegistry.registerEntityRenderingHandler(LightEntity.class, entityRendererManager -> new EntityRenderer<LightEntity>(entityRendererManager) {
                @Nullable
                @Override
                protected ResourceLocation getEntityTexture(LightEntity entity) {
                    return null;
                }
            });
        });

        // Outside of the entity iteration loop
        ClientEvents.TICK.subscribe(() -> {
            toSpawn.forEach(Runnable::run);
            toSpawn.clear();

            if (!hasRegistered) {
                if (OptiFine.isLoaded()) {
                    // I think optifine is not calling into forge mod list correctly.  Let's force this shit.
                    try {
                        Class<?> dl = Class.forName("net.optifine.DynamicLights");
                        Field initField = dl.getDeclaredField("initialized");
                        initField.setAccessible(true);
                        if (initField.getBoolean(null)) {
                            // Wait for optifine to do it's own broken loading
                            Method lmc = dl.getDeclaredMethod("loadModConfiguration", InputStream.class, String.class, String.class);
                            lmc.setAccessible(true);
                            Identifier id = new Identifier("universalmodcore", "optifine/dynamic_lights.properties");
                            lmc.invoke(null, id.getResourceStream(), id.toString(), id.getDomain());
                            hasRegistered = true;
                        }
                    } catch (Exception e) {
                        ModCore.catching(e);
                        hasRegistered = true;
                    }
                } else {
                    hasRegistered = true;
                }
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
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
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
            Class<?> optiConfig = Class.forName("net.optifine.Config");
            return Objects.equals(true, optiConfig.getDeclaredMethod("isDynamicLights").invoke(null));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            System.out.println("NOT ENABLED");
            return false;
        }
    }
}
