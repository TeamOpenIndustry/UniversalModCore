package cam72cam.mod.render;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Light {
    private LightEntity internal;
    private double lightLevel;
    private static final Map<ChunkPos, List<LightEntity>> CHUNK_LIGHTS = new HashMap<>();
    private static final Object LOCK = new Object();
    private static void registerInChunk(LightEntity entity) {
        if (entity == null) return;
        int cx = (int) Math.floor(entity.posX) >> 4;
        int cz = (int) Math.floor(entity.posZ) >> 4;
        ChunkPos cp = new ChunkPos(cx, cz);
        synchronized (LOCK) {
            CHUNK_LIGHTS.computeIfAbsent(cp, k -> new ArrayList<>()).add(entity);
        }
    }
    private static void unregisterFromChunk(LightEntity entity) {
        if (entity == null) return;
        int cx = (int) Math.floor(entity.posX) >> 4;
        int cz = (int) Math.floor(entity.posZ) >> 4;
        ChunkPos cp = new ChunkPos(cx, cz);
        synchronized (LOCK) {
            List<LightEntity> list = CHUNK_LIGHTS.get(cp);
            if (list != null) {
                list.remove(entity);
                if (list.isEmpty()) CHUNK_LIGHTS.remove(cp);
            }
        }
    }
    public static List<LightInfo> getLightsInRange(Vec3d center, double radius) {
        int minX = (int)Math.floor((center.x - radius) / 16);
        int maxX = (int)Math.floor((center.x + radius) / 16);
        int minZ = (int)Math.floor((center.z - radius) / 16);
        int maxZ = (int)Math.floor((center.z + radius) / 16);
        List<LightInfo> result = new ArrayList<>();
        synchronized (LOCK) {
            for (int cx = minX; cx <= maxX; cx++) {
                for (int cz = minZ; cz <= maxZ; cz++) {
                    ChunkPos cp = new ChunkPos(cx, cz);
                    List<LightEntity> entities = CHUNK_LIGHTS.get(cp);
                    if (entities != null) {
                        for (LightEntity e : entities) {
                            if (e.isDead) continue;//can avoid some ghost entities?
                            double dx = e.posX - center.x;
                            double dy = e.posY - center.y;
                            double dz = e.posZ - center.z;
                            if (dx*dx + dy*dy + dz*dz <= radius*radius) {
                                result.add(new LightInfo(new Vec3d(e.posX, e.posY, e.posZ), e.getSimulateLightLevel()));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
    public static class LightInfo {
        public final Vec3d pos;
        public final double level;
        public LightInfo(Vec3d pos, double level) {
            this.pos = pos;
            this.level = level;
        }
    }

    public Light(World world, Vec3d pos, double lightLevel) {
        init(world.internal, pos.internal(), lightLevel);
    }

    public void remove() {
        if (internal != null) {
            unregisterFromChunk(internal);
            internal.setDead();
            internal = null;
        }
    }

    public void setPosition(Vec3d pos) {
        if (internal == null) return;
        unregisterFromChunk(internal);
        internal.setPosition(pos.x, pos.y, pos.z);
        registerInChunk(internal);
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
            unregisterFromChunk(internal);
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

        internal.setSimulateLightLevel(lightLevel);
        registerInChunk(internal);

        internal.setPosition(pos.x, pos.y, pos.z);
        world.spawnEntity(internal);
        this.lightLevel = lightLevel;
    }

    private static class LightEntity1 extends LightEntity {public LightEntity1(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity2 extends LightEntity {public LightEntity2(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity3 extends LightEntity {public LightEntity3(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity4 extends LightEntity {public LightEntity4(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity5 extends LightEntity {public LightEntity5(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity6 extends LightEntity {public LightEntity6(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity7 extends LightEntity {public LightEntity7(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity8 extends LightEntity {public LightEntity8(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity9 extends LightEntity {public LightEntity9(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity10 extends LightEntity {public LightEntity10(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity11 extends LightEntity {public LightEntity11(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity12 extends LightEntity {public LightEntity12(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity13 extends LightEntity {public LightEntity13(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity14 extends LightEntity {public LightEntity14(net.minecraft.world.World world) { super(world);}}
    private static class LightEntity15 extends LightEntity {public LightEntity15(net.minecraft.world.World world) { super(world);}}

    public static void register() {
        CommonEvents.Entity.REGISTER.subscribe(() -> {
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light1"), LightEntity1.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light2"), LightEntity2.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light3"), LightEntity3.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light4"), LightEntity4.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light5"), LightEntity5.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light6"), LightEntity6.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light7"), LightEntity7.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light8"), LightEntity8.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light9"), LightEntity9.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light10"), LightEntity10.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light11"), LightEntity11.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light12"), LightEntity12.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light13"), LightEntity13.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light14"), LightEntity14.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation("universalmodcore:light15"), LightEntity15.class, "LightEntity", -1, ModCore.instance, 0, 0, false);
        });
    }

    // Client only
    private static class LightEntity extends Entity {
        private double simulateLightLevel;
        public LightEntity(net.minecraft.world.World world) {
            super(world);
            super.width = 0;
            super.height = 0;
            super.isImmuneToFire = true;
            super.noClip = true;
        }

        public void setSimulateLightLevel(double level) {
            this.simulateLightLevel = level;
        }

        public double getSimulateLightLevel() {
            return simulateLightLevel;
        }

        @Override
        public void onEntityUpdate() {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
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

        @Override
        public void setDead() {//can avoid some ghost entities?
            unregisterFromChunk(this);
            super.setDead();
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
