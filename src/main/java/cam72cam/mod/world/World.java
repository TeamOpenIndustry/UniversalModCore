package cam72cam.mod.world;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockType;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Living;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.TagCompound;
import net.minecraft.block.*;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class World {

    /* Static access to loaded worlds */
    private static Map<net.minecraft.world.World, World> clientWorlds = new HashMap<>();
    private static Map<net.minecraft.world.World, World> serverWorlds = new HashMap<>();
    private static Map<Integer, World> clientWorldsByID = new HashMap<>();
    private static Map<Integer, World> serverWorldsByID = new HashMap<>();
    private static List<Consumer<World>> onTicks = new ArrayList<>();

    public final net.minecraft.world.World internal;
    public final boolean isClient;
    public final boolean isServer;
    private final List<Entity> entities;
    private final Map<Integer, Entity> entityByID;
    private final Map<UUID, Entity> entityByUUID;
    private long ticks;

    /* World Initialization */

    private World(net.minecraft.world.World world) {
        internal = world;
        isClient = world.isRemote;
        isServer = !world.isRemote;
        entities = new ArrayList<>();
        entityByID = new HashMap<>();
        entityByUUID = new HashMap<>();
    }

    private static void loadWorld(net.minecraft.world.World world) {
        Map<net.minecraft.world.World, World> worlds = world.isRemote ? clientWorlds : serverWorlds;
        if (worlds.containsKey(world)) {
            return;
        }

        Map<Integer, World> worldsByID = world.isRemote ? clientWorldsByID : serverWorldsByID;

        World worldWrap = new World(world);
        worlds.put(world, worldWrap);
        worldsByID.put(worldWrap.getId(), worldWrap);

        world.addWorldAccess(new WorldEventListener(worldWrap));
    }

    public static void registerEvents() {
        CommonEvents.World.LOAD.subscribe(World::loadWorld);

        CommonEvents.World.UNLOAD.subscribe(world -> {
            Map<net.minecraft.world.World, World> worlds = world.isRemote ? clientWorlds : serverWorlds;
            Map<Integer, World> worldsByID = world.isRemote ? clientWorldsByID : serverWorldsByID;

            worlds.remove(world);
            worldsByID.remove(world.provider.dimensionId);
        });

        CommonEvents.World.TICK.subscribe(worldIn -> {
            World world = get(worldIn);
            onTicks.forEach(fn -> fn.accept(world));
            world.ticks++;

            world.entityByID.entrySet().stream()
                    .filter(x -> x.getKey() != x.getValue().getId()).collect(Collectors.toList())
                    .forEach(x -> {world.entityByID.remove(x.getKey()); System.out.println("BAD ENT " + x);});

            world.entityByUUID.entrySet().stream()
                    .filter(x -> !x.getKey().equals(x.getValue().getUUID())).collect(Collectors.toList())
                    .forEach(x -> {world.entityByUUID.remove(x.getKey()); System.out.println("BAD ENT " + x);});

            for (Entity entity : world.entities) {
                if (!world.entityByID.containsKey(entity.getId())) {
                    world.entityByID.put(entity.getId(), entity);
                }
                if (!world.entityByUUID.containsKey(entity.getUUID())) {
                    world.entityByUUID.put(entity.getUUID(), entity);
                }
            }
        });
    }

    public static World get(net.minecraft.world.World world) {
        if (world == null) {
            return null;
        }
        Map<net.minecraft.world.World, World> worlds = world.isRemote ? clientWorlds : serverWorlds;
        if (!worlds.containsKey(world)) {
            // WTF forge
            // I should NOT need to do this
            loadWorld(world);
        }

        return worlds.get(world);
    }

    public static World get(int dimID, boolean isClient) {
        Map<Integer, World> worldsByID = isClient ? clientWorldsByID : serverWorldsByID;

        return worldsByID.get(dimID);
    }

    public static void onTick(Consumer<World> fn) {
        onTicks.add(fn);
    }

    public int getId() {
        return internal.provider.dimensionId;
    }

    public boolean doesBlockCollideWith(Vec3i bp, IBoundingBox bb) {
        IBoundingBox bbb = IBoundingBox.from(internal.getBlock(bp.x, bp.y, bp.z).getCollisionBoundingBoxFromPool(internal, bp.x, bp.y, bp.z));
        return bbb != null && bb.intersects(bbb);
    }

    /* Event Methods */

    void onEntityAdded(net.minecraft.entity.Entity entityIn) {
        Entity entity;
        if (entityIn instanceof ModdedEntity) {
            entity = ((ModdedEntity) entityIn).getSelf();
        } else if (entityIn instanceof EntityPlayer) {
            entity = new Player((EntityPlayer) entityIn);
        } else if (entityIn instanceof EntityLiving) {
            entity = new Living((EntityLiving) entityIn);
        } else {
            entity = new Entity(entityIn);
        }
        entities.add(entity);
        entityByID.put(entityIn.getEntityId(), entity);
        entityByUUID.put(entity.getUUID(), entity);

    }

    void onEntityRemoved(net.minecraft.entity.Entity entity) {
        entities.stream().filter(x -> x.getUUID().equals(entity.getUniqueID())).findFirst().ifPresent(entities::remove);
        entityByID.remove(entity.getEntityId());
        entityByUUID.remove(entity.getUniqueID());
    }

    /* Entity Methods */

    public Entity getEntity(net.minecraft.entity.Entity entity) {
        return getEntity(entity.getUniqueID(), Entity.class);
    }

    public <T extends Entity> T getEntity(int id, Class<T> type) {
        Entity ent = entityByID.get(id);
        if (ent == null) {
            return null;
        }
        if (!type.isInstance(ent)) {
            ModCore.warn("When looking for entity %s by id %s, we instead got a %s", type, id, ent.getClass());
            return null;
        }
        return (T) ent;
    }

    public <T extends Entity> T getEntity(UUID id, Class<T> type) {
        Entity ent = entityByUUID.get(id);
        if (ent == null) {
            return null;
        }
        if (!type.isInstance(ent)) {
            ModCore.warn("When looking for entity %s by id %s, we instead got a %s", type, id, ent.getClass());
            return null;
        }
        return (T) ent;
    }

    public <T extends Entity> List<T> getEntities(Class<T> type) {
        return getEntities((T val) -> true, type);
    }

    public <T extends Entity> List<T> getEntities(Predicate<T> filter, Class<T> type) {
        return entities.stream().map(entity -> entity.as(type)).filter(Objects::nonNull).filter(filter).collect(Collectors.toList());
    }

    public boolean spawnEntity(Entity ent) {
        return internal.spawnEntityInWorld(ent.internal);
    }


    public void keepLoaded(Vec3i pos) {
        ChunkManager.flagEntityPos(this, pos);
    }


    public <T extends BlockEntity> List<T> getBlockEntities(Class<T> cls) {
        return ((List<net.minecraft.tileentity.TileEntity>)internal.loadedTileEntityList).stream()
                .filter(x -> x instanceof cam72cam.mod.block.tile.TileEntity && ((TileEntity) x).isLoaded() && cls.isInstance(((TileEntity) x).instance()))
                .map(x -> (T) ((TileEntity) x).instance())
                .collect(Collectors.toList());
    }

    public <T extends net.minecraft.tileentity.TileEntity> T getTileEntity(Vec3i pos, Class<T> cls) {
        return getTileEntity(pos, cls, true);
    }

    public <T extends net.minecraft.tileentity.TileEntity> T getTileEntity(Vec3i pos, Class<T> cls, boolean create) {
        net.minecraft.tileentity.TileEntity ent;
        ent = internal.getChunkFromBlockCoords(pos.x, pos.z).getTileEntityUnsafe(pos.x, pos.y, pos.z);
        if (ent == null && create) {
            ent = internal.getTileEntity(pos.x, pos.y, pos.z);
        }
        if (cls.isInstance(ent)) {
            return (T) ent;
        }
        return null;
    }

    public <T extends BlockEntity> T getBlockEntity(Vec3i pos, Class<T> cls) {
        TileEntity te = getTileEntity(pos, TileEntity.class);
        if (te == null) {
            return null;
        }
        BlockEntity instance = te.instance();
        if (cls.isInstance(instance)) {
            return (T) instance;
        }
        return null;
    }

    public <T extends BlockEntity> boolean hasBlockEntity(Vec3i pos, Class<T> cls) {
        TileEntity te = getTileEntity(pos, TileEntity.class);
        if (te == null) {
            return false;
        }
        return cls.isInstance(te.instance());
    }

    public BlockEntity reconstituteBlockEntity(TagCompound data) {
        TileEntity te = (TileEntity) TileEntity.createAndLoadEntity(data.internal);
        if (te == null) {
            System.out.println("BAD TE DATA " + data);
            return null;
        }
        te.setWorldObj(internal);
        if (te.instance() == null) {
            System.out.println("Loaded " + te.isLoaded() + " " + data);
        }
        return te.instance();
    }

    public void setBlockEntity(Vec3i pos, BlockEntity entity) {
        internal.setTileEntity(pos.x, pos.y, pos.z, entity.internal);
        entity.markDirty();
    }

    public void setToAir(Vec3i pos) {
        internal.setBlockToAir(pos.x, pos.y, pos.z);
    }

    public long getTime() {
        return internal.getWorldTime();
    }

    public long getTicks() {
        return ticks;
    }

    public double getTPS(int sampleSize) {

        if (MinecraftServer.getServer() == null) {
            return 20;
        }

        long[] ttl = MinecraftServer.getServer().worldTickTimes.get(internal.provider.dimensionId);

        sampleSize = Math.min(sampleSize, ttl.length);
        double ttus = 0;
        for (int i = 0; i < sampleSize; i++) {
            ttus += ttl[ttl.length - 1 - i] / (double) sampleSize;
        }

        if (ttus == 0) {
            ttus = 0.01;
        }

        double ttms = ttus * 1.0E-6D;
        return Math.min(1000.0 / ttms, 20);
    }

    public Vec3i getPrecipitationHeight(Vec3i pos) {
        return new Vec3i(pos.x, internal.getPrecipitationHeight(pos.x, pos.z), pos.z);
    }

    public boolean isAir(Vec3i pos) {
        return internal.isAirBlock(pos.x, pos.y, pos.z);
    }

    public void setSnowLevel(Vec3i pos, int snowDown) {
        snowDown = Math.max(1, Math.min(8, snowDown));
        internal.setBlock(pos.x, pos.y, pos.z, Blocks.snow_layer, snowDown, 3);
    }

    public int getSnowLevel(Vec3i pos) {
        Block block = internal.getBlock(pos.x, pos.y, pos.z);
        return block instanceof BlockSnow ? internal.getBlockMetadata(pos.x, pos.y, pos.z) & 7 : block instanceof BlockSnowBlock ? 8 : 0;
    }

    public boolean isSnow(Vec3i pos) {
        Block block = internal.getBlock(pos.x, pos.y, pos.z);
        return block instanceof BlockSnowBlock;
    }

    public boolean isSnowBlock(Vec3i pos) {
        Block block = internal.getBlock(pos.x, pos.y, pos.z);
        return block instanceof BlockSnowBlock;
    }

    public boolean isPrecipitating() {
        return internal.isRaining();
    }

    public boolean isBlockLoaded(Vec3i pos) {
        return internal.getChunkProvider().chunkExists(pos.x >> 4, pos.z >> 4);
    }

    public void breakBlock(Vec3i pos) {
        this.breakBlock(pos, true);
    }

    public void breakBlock(Vec3i pos, boolean drop) {
        internal.func_147480_a(pos.x, pos.y, pos.z, drop);
    }

    public void dropItem(ItemStack stack, Vec3i pos) {
        dropItem(stack, new Vec3d(pos));
    }

    public void dropItem(ItemStack stack, Vec3d pos) {
        internal.spawnEntityInWorld(new EntityItem(internal, pos.x, pos.y, pos.z, stack.internal));
    }

    public void setBlock(Vec3i pos, BlockType block) {
        internal.setBlock(pos.x, pos.y, pos.z, block.internal);
    }

    public void setBlock(Vec3i pos, ItemStack stack) {
        if (stack.isEmpty()) {
            internal.setBlockToAir(pos.x, pos.y, pos.z);
        } else {
            internal.setBlock(pos.x, pos.y, pos.z, Block.getBlockFromItem(stack.internal.getItem()), stack.internal.getItemDamage(), 3);
        }
    }

    public boolean isTopSolid(Vec3i pos) {
        return internal.getBlock(pos.x, pos.y, pos.z).isSideSolid(internal, pos.x, pos.y, pos.z, ForgeDirection.UP);
    }

    public int getRedstone(Vec3i pos) {
        return internal.getStrongestIndirectPower(pos.x, pos.y, pos.z);
    }

    public void removeEntity(cam72cam.mod.entity.Entity entity) {
        internal.removeEntity(entity.internal);
    }

    public boolean canSeeSky(Vec3i pos) {
        return internal.canBlockSeeTheSky(pos.x, pos.y, pos.z);
    }

    public boolean isRaining(Vec3i pos) {
        return internal.getBiomeGenForCoords(pos.x, pos.z).canSpawnLightningBolt();
    }

    public boolean isSnowing(Vec3i pos) {
        return internal.getBiomeGenForCoords(pos.x, pos.z).getEnableSnow();
    }

    public float getTemperature(Vec3i pos) {
        float mctemp = internal.getBiomeGenForCoords(pos.x, pos.z).temperature;
        //https://www.reddit.com/r/Minecraft/comments/3eh7yu/the_rl_temperature_of_minecraft_biomes_revealed/ctex050/
        return (13.6484805403f * mctemp) + 7.0879687222f;
    }

    public boolean isBlock(Vec3i pos, BlockType block) {
        return internal.getBlock(pos.x, pos.y, pos.z) == block.internal;
    }

    public boolean isReplacable(Vec3i pos) {
        if (isAir(pos)) {
            return true;
        }

        Block block = internal.getBlock(pos.x, pos.y, pos.z);

        if (block.isReplaceable(internal, pos.x, pos.y, pos.z)) {
            return true;
        }
        if (block instanceof IGrowable && !(block instanceof BlockGrass)) {
            return true;
        }
        if (block instanceof IPlantable) {
            return true;
        }
        if (block instanceof BlockLiquid) {
            return true;
        }
        if (block instanceof BlockSnow) {
            return true;
        }
        if (block instanceof BlockLeaves) {
            return true;
        }
        return false;
    }

    /* Capabilities */

    public IInventory getInventory(Vec3i pos) {
        net.minecraft.tileentity.TileEntity te = internal.getTileEntity(pos.x, pos.y, pos.z);
        if (te instanceof ISidedInventory) {
            return IInventory.from((net.minecraft.inventory.IInventory) te);
        }
        return null;
    }

    public ITank getTank(Vec3i pos) {
        net.minecraft.tileentity.TileEntity te = internal.getTileEntity(pos.x, pos.y, pos.z);
        if (te instanceof IFluidHandler) {
                return ITank.getTank((IFluidHandler)te);
        }
        return null;
    }

    public ItemStack getItemStack(Vec3i pos) {
        Block state = internal.getBlock(pos.x, pos.y, pos.z);
        try {
            return new ItemStack(state.getPickBlock(new MovingObjectPosition(pos.x, pos.y, pos.z, 1, Vec3.createVectorHelper(0, 0, 0)), internal, pos.x, pos.y, pos.z));
        } catch (NoSuchMethodError ex) {
            return new ItemStack(Item.getItemFromBlock(state));
        }
    }

    public List<ItemStack> getDroppedItems(IBoundingBox bb) {
        List<EntityItem> items = internal.getEntitiesWithinAABB(EntityItem.class, new BoundingBox(bb).expand(0, 1, 0));
        return items.stream().map((EntityItem::getEntityItem)).map(ItemStack::new).collect(Collectors.toList());
    }

    public BlockInfo getBlock(Vec3i pos) {
        return new BlockInfo(internal.getBlock(pos.x, pos.y, pos.z), internal.getBlockMetadata(pos.x, pos.y, pos.z));
    }

    public void setBlock(Vec3i pos, BlockInfo info) {
        internal.removeTileEntity(pos.x, pos.y, pos.z);
        if (info == null || info.internal == null) {
            internal.setBlockToAir(pos.x, pos.y, pos.z);
        } else {
            internal.setBlock(pos.x, pos.y, pos.z, info.internal, info.internalMeta, 3);
        }
    }

    public boolean canEntityCollideWith(Vec3i pos, String damageType) {
        Block block = internal.getBlock(pos.x, pos.y, pos.z);
        return !(block instanceof IConditionalCollision) ||
                ((IConditionalCollision) block).canCollide(internal, pos.x, pos.y, pos.z, new DamageSource(damageType));
    }

    public void createParticle(ParticleType type, Vec3d position, Vec3d velocity) {
        internal.spawnParticle(type.internal, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
    }

    public enum ParticleType {
        SMOKE("smoke"),
        ;

        private final String internal;

        ParticleType(String internal) {
            this.internal = internal;
        }
    }
}
