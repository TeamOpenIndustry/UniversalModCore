package cam72cam.mod.render;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class Light {
    private LightEntity internal;
    private double lightLevel;

    public Light(World world, Vec3d pos, double lightLevel) {
        init(world.internal, pos.x, pos.y, pos.z, lightLevel);
    }

    public void remove() {
        internal.setDead();
        internal = null;
    }

    public void setPosition(Vec3d pos) {
        internal.setPosition(pos.x, pos.y, pos.z);
    }

    public void setLightLevel(double lightLevel) {
        init(internal.worldObj, internal.posX, internal.posY, internal.posZ, lightLevel);
    }

    private void init(net.minecraft.world.World world, double x, double y, double z, double lightLevel) {
        if (lightLevel == this.lightLevel) {
            // NOP
            return;
        }
        if (internal != null) {
            internal.setDead();
        }
        switch ((int) Math.ceil((lightLevel * 15))) {
            case 1: internal = new LightEntity1(world); break;
            case 2: internal = new LightEntity2(world); break;
            case 3: internal = new LightEntity3(world); break;
            case 4: internal = new LightEntity4(world); break;
            case 5: internal = new LightEntity5(world); break;
            case 6: internal = new LightEntity6(world); break;
            case 7: internal = new LightEntity7(world); break;
            case 8: internal = new LightEntity8(world); break;
            case 9: internal = new LightEntity9(world); break;
            case 10: internal = new LightEntity10(world); break;
            case 11: internal = new LightEntity11(world); break;
            case 12: internal = new LightEntity12(world); break;
            case 13: internal = new LightEntity13(world); break;
            case 14: internal = new LightEntity14(world); break;
            default:
            case 15: internal = new LightEntity15(world); break;
        }
        internal.setPosition(x, y, z);
        world.spawnEntityInWorld(internal);
        this.lightLevel = lightLevel;
    }

    private static class LightEntity1 extends LightEntity {public LightEntity1(net.minecraft.world.World world) { super(world, 1);}}
    private static class LightEntity2 extends LightEntity {public LightEntity2(net.minecraft.world.World world) { super(world, 2);}}
    private static class LightEntity3 extends LightEntity {public LightEntity3(net.minecraft.world.World world) { super(world, 3);}}
    private static class LightEntity4 extends LightEntity {public LightEntity4(net.minecraft.world.World world) { super(world, 4);}}
    private static class LightEntity5 extends LightEntity {public LightEntity5(net.minecraft.world.World world) { super(world, 5);}}
    private static class LightEntity6 extends LightEntity {public LightEntity6(net.minecraft.world.World world) { super(world, 6);}}
    private static class LightEntity7 extends LightEntity {public LightEntity7(net.minecraft.world.World world) { super(world, 7);}}
    private static class LightEntity8 extends LightEntity {public LightEntity8(net.minecraft.world.World world) { super(world, 8);}}
    private static class LightEntity9 extends LightEntity {public LightEntity9(net.minecraft.world.World world) { super(world, 9);}}
    private static class LightEntity10 extends LightEntity {public LightEntity10(net.minecraft.world.World world) { super(world, 10);}}
    private static class LightEntity11 extends LightEntity {public LightEntity11(net.minecraft.world.World world) { super(world, 11);}}
    private static class LightEntity12 extends LightEntity {public LightEntity12(net.minecraft.world.World world) { super(world, 12);}}
    private static class LightEntity13 extends LightEntity {public LightEntity13(net.minecraft.world.World world) { super(world, 13);}}
    private static class LightEntity14 extends LightEntity {public LightEntity14(net.minecraft.world.World world) { super(world, 14);}}
    private static class LightEntity15 extends LightEntity {public LightEntity15(net.minecraft.world.World world) { super(world, 15);}}

    public static void register() {
        CommonEvents.Entity.REGISTER.subscribe(() -> {
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity1.class, "light1", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity2.class, "light2", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity3.class, "light3", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity4.class, "light4", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity5.class, "light5", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity6.class, "light6", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity7.class, "light7", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity8.class, "light8", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity9.class, "light9", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity10.class, "light10", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity11.class, "light11", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity12.class, "light12", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity13.class, "light13", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity14.class, "light14", -1, ModCore.instance, 0, 0, false);
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(LightEntity15.class, "light15", -1, ModCore.instance, 0, 0, false);
        });
    }

    // Client only
    private static class LightEntity extends Entity {
        private final int lightValue;

        public LightEntity(net.minecraft.world.World world, int lightValue) {
            super(world);
            super.width = 0;
            super.height = 0;
            super.isImmuneToFire = true;
            super.noClip = true;
            this.lightValue = lightValue;
        }

        @Override
        public void onEntityUpdate() {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            Angelica.updateLights(this);
        }

        @Override
        protected void entityInit() {

        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound compound) {

        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound compound) {

        }

        public int angelica$getLuminance() {
            return lightValue;
        }
    }

    private static final class Angelica {
        private static final Method UPDATE_LIGHTS;
        private static final Method IS_ENABLED;
        private static boolean failed;

        static {
            Method update = null, enabled = null;
            try {
                Class<?> dl = Class.forName("com.gtnewhorizons.angelica.dynamiclights.DynamicLights",
                        false, Light.class.getClassLoader());
                Method e = dl.getDeclaredMethod("isEnabled");
                e.setAccessible(true);
                update = Entity.class.getMethod("angelica$updateLights");
                enabled = e;
            } catch (Throwable ignored) {
                update = null;
            }
            UPDATE_LIGHTS = update;
            IS_ENABLED = enabled;
        }

        static boolean isActive() {
            if (UPDATE_LIGHTS == null || IS_ENABLED == null || failed) return false;
            try {
                return Boolean.TRUE.equals(IS_ENABLED.invoke(null));
            } catch (Throwable t) {
                return false;
            }
        }

        static void updateLights(Entity e) {
            if (UPDATE_LIGHTS == null || failed) return;
            try {
                UPDATE_LIGHTS.invoke(e);
            } catch (Exception ex) {
                failed = true;
                ModCore.catching(ex);
            }
        }
    }

    public static boolean enabled() {
        return optifineDynamicLights() || Angelica.isActive();
}

    private static boolean optifineDynamicLights() {
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