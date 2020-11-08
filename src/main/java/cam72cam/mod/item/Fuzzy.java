package cam72cam.mod.item;

import cam72cam.mod.config.ConfigFile;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** OreDict / Tag abstraction.  Use for item equivalence */
public class Fuzzy {
    static Map<String, Fuzzy> tags = new HashMap<>();

    // https://github.com/Dente222/Minecraft-Forge-Tag-List/blob/master/ingots.txt
    public static final Fuzzy WOOD_STICK = new Fuzzy("stickWood").addAll(new Fuzzy(Tags.Items.RODS_WOODEN.getId().toString()));
    public static final Fuzzy WOOD_PLANK = new Fuzzy("plankWood").addAll(new Fuzzy(ItemTags.PLANKS.getId().toString()));
    public static final Fuzzy REDSTONE_DUST = new Fuzzy("dustRedstone").addAll(new Fuzzy(Tags.Items.DUSTS_REDSTONE.getId().toString()));
    public static final Fuzzy SNOW_LAYER = new Fuzzy("layerSnow").add(Blocks.SNOW);
    public static final Fuzzy SNOW_BLOCK = new Fuzzy("blockSnow").add(Blocks.SNOW_BLOCK);
    public static final Fuzzy LEAD = new Fuzzy("lead").add(Items.LEAD);

    public static final Fuzzy STONE_SLAB = new Fuzzy("slabStone").addAll(new Fuzzy(ItemTags.SLABS.getId().toString()));
    public static final Fuzzy STONE_BRICK = new Fuzzy("brickStone").addAll(new Fuzzy(ItemTags.STONE_BRICKS.getId().toString()));
    public static final Fuzzy SAND = new Fuzzy("sand").addAll(new Fuzzy(Tags.Items.SAND.getId().toString()));
    public static final Fuzzy PISTON = new Fuzzy("piston").add(Items.PISTON);

    public static final Fuzzy GOLD_INGOT = new Fuzzy("ingotGold").addAll(new Fuzzy(Tags.Items.INGOTS_GOLD.getId().toString()));
    public static final Fuzzy STEEL_INGOT = new Fuzzy("ingotSteel").addAll(new Fuzzy("forge:ingots/steel"));
    public static final Fuzzy STEEL_BLOCK = new Fuzzy("blockSteel").addAll(new Fuzzy("forge:storage_blocks/steel"));
    public static final Fuzzy IRON_INGOT = new Fuzzy("ingotIron").addAll(new Fuzzy(Tags.Items.INGOTS_IRON.getId().toString()));
    public static final Fuzzy IRON_BLOCK = new Fuzzy("blockIron").addAll(new Fuzzy(Tags.Items.STORAGE_BLOCKS_IRON.getId().toString()));
    public static final Fuzzy IRON_BARS = new Fuzzy("barsIron").add(Blocks.IRON_BARS);

    public static final Fuzzy NETHER_BRICK = new Fuzzy("brickNether").add(Blocks.NETHER_BRICKS);
    public static final Fuzzy GRAVEL_BLOCK = new Fuzzy("gravel").addAll(new Fuzzy(Tags.Items.GRAVEL.getId().toString()));
    public static final Fuzzy BRICK_BLOCK = new Fuzzy("brickBlock").add(Blocks.BRICKS);
    public static final Fuzzy COBBLESTONE = new Fuzzy("cobblestone").addAll(new Fuzzy(Tags.Items.COBBLESTONE.getId().toString()));
    public static final Fuzzy CONCRETE = new Fuzzy("concrete")
            .add(Blocks.WHITE_CONCRETE)
            .add(Blocks.ORANGE_CONCRETE)
            .add(Blocks.MAGENTA_CONCRETE)
            .add(Blocks.LIGHT_BLUE_CONCRETE)
            .add(Blocks.YELLOW_CONCRETE)
            .add(Blocks.LIME_CONCRETE)
            .add(Blocks.PINK_CONCRETE)
            .add(Blocks.GRAY_CONCRETE)
            .add(Blocks.LIGHT_GRAY_CONCRETE)
            .add(Blocks.CYAN_CONCRETE)
            .add(Blocks.PURPLE_CONCRETE)
            .add(Blocks.BLUE_CONCRETE)
            .add(Blocks.BROWN_CONCRETE)
            .add(Blocks.GREEN_CONCRETE)
            .add(Blocks.RED_CONCRETE)
            .add(Blocks.BLACK_CONCRETE);


    public static final Fuzzy DIRT = new Fuzzy("dirt").addAll(new Fuzzy(Tags.Blocks.DIRT.getId().toString()));
    public static final Fuzzy HARDENED_CLAY = new Fuzzy("hardened_clay")
            .add(Blocks.WHITE_TERRACOTTA)
            .add(Blocks.ORANGE_TERRACOTTA)
            .add(Blocks.MAGENTA_TERRACOTTA)
            .add(Blocks.LIGHT_BLUE_TERRACOTTA)
            .add(Blocks.YELLOW_TERRACOTTA)
            .add(Blocks.LIME_TERRACOTTA)
            .add(Blocks.PINK_TERRACOTTA)
            .add(Blocks.GRAY_TERRACOTTA)
            .add(Blocks.LIGHT_GRAY_TERRACOTTA)
            .add(Blocks.CYAN_TERRACOTTA)
            .add(Blocks.PURPLE_TERRACOTTA)
            .add(Blocks.BLUE_TERRACOTTA)
            .add(Blocks.BROWN_TERRACOTTA)
            .add(Blocks.GREEN_TERRACOTTA)
            .add(Blocks.RED_TERRACOTTA)
            .add(Blocks.BLACK_TERRACOTTA);

    public static final Fuzzy LOG_WOOD = new Fuzzy("logWood").addAll(new Fuzzy(ItemTags.LOGS.getId().toString()));
    public static final Fuzzy PAPER = new Fuzzy("paper").add(Items.PAPER);
    public static final Fuzzy BOOK = new Fuzzy("book").add(Items.BOOK);
    public static final Fuzzy WOOL_BLOCK = new Fuzzy("wool").addAll(new Fuzzy(ItemTags.WOOL.getId().toString()));
    public static final Fuzzy BUCKET = new Fuzzy("bucket").add(Items.BUCKET);
    public static final Fuzzy EMERALD = new Fuzzy("gemEmerald").addAll(new Fuzzy(Tags.Items.GEMS_EMERALD.getId().toString()));
    public static final Fuzzy REDSTONE_TORCH = new Fuzzy("redstoneTorch").add(Blocks.REDSTONE_TORCH);
    public static final Fuzzy GLASS_PANE = new Fuzzy("paneGlass").addAll(new Fuzzy(Tags.Items.GLASS_PANES.getId().toString()));

    static {
        ConfigFile.addMapper(Fuzzy.class, Fuzzy::toString, Fuzzy::new);
    }



    private final String ident;
    private List<Item> customItems = new ArrayList<>();
    private List<Fuzzy> subTags = new ArrayList<>();

    /** Create fuzzy with this name */
    public Fuzzy(String ident) {
        this.ident = ident;
        if (tags.containsKey(ident)) {
            this.customItems = tags.get(ident).customItems;
            this.subTags = tags.get(ident).subTags;
        } else {
            tags.put(ident, this);
        }
    }

    private List<Item> values() {
        List<Item> values = new ArrayList<>(customItems);
        Tag<Item> registered = ItemTags.getCollection().get(new ResourceLocation(ident.toLowerCase()));
        if (registered != null) {
            values.addAll(registered.getAllElements());
        }

        subTags.stream().map(Fuzzy::values).forEach(values::addAll);

        return values;
    }

    /** Is the item in this stack matched by this fuzzy? */
    public boolean matches(ItemStack stack) {
        return values().stream().anyMatch(potential -> potential == stack.internal.getItem());
    }

    /** List all possible itemstacks */
    public List<ItemStack> enumerate() {
        return values().stream().map(item -> new ItemStack(new net.minecraft.item.ItemStack(item))).collect(Collectors.toList());
    }

    /** Grab the first example of a item in this fuzzy */
    public ItemStack example() {
        List<ItemStack> stacks = enumerate();
        return stacks.size() != 0 ? stacks.get(0) : ItemStack.EMPTY;
    }

    /** Use to register an itemstack */
    public Fuzzy add(ItemStack item) {
        add(item.internal.getItem());
        return this;
    }

    /** Don't use directly (unless in version specific code) */
    public Fuzzy add(Block block) {
        add(block.asItem());
        return this;
    }

    /** Don't use directly (unless in version specific code) */
    public Fuzzy add(Item item) {
        customItems.add(item);
        return this;
    }

    /** Use to register an item */
    public void add(CustomItem item) {
        add(item.internal);
    }

    /** Copy all from this other fuzzy into this one.  Does not track updates to other */
    public Fuzzy addAll(Fuzzy other) {
        if (!other.ident.equals(this.ident)) {
            subTags.add(other);
        }
        return this;
    }

    @Override
    public String toString() {
        return ident;
    }
}
