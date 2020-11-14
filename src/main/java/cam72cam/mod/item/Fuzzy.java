package cam72cam.mod.item;

import cam72cam.mod.config.ConfigFile;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.fabricmc.fabric.api.tools.FabricToolTags;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.server.ItemTagsProvider;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OreDict / Tag abstraction.  Use for item equivalence
 */
public class Fuzzy {
    private static Tag<Item> getTag(String name) {
        return new ItemTags.CachingTag(new Identifier(name));
    }

    public static final Fuzzy WOOD_STICK = new Fuzzy(getTag("c:wood_sticks"), "stickWood").add(Items.STICK);
    public static final Fuzzy WOOD_PLANK = new Fuzzy(ItemTags.PLANKS, "plankWood");
    public static final Fuzzy REDSTONE_DUST = new Fuzzy(getTag("c:redstone_dusts"), "dustRedstone").add(Items.REDSTONE);
    public static final Fuzzy SNOW_LAYER = new Fuzzy("layerSnow").add(Blocks.SNOW);
    public static final Fuzzy SNOW_BLOCK = new Fuzzy("blockSnow").add(Blocks.SNOW_BLOCK);
    public static final Fuzzy LEAD = new Fuzzy("lead").add(Items.LEAD);

    public static final Fuzzy STONE_SLAB = new Fuzzy("slabStone").add(Items.STONE_SLAB);
    public static final Fuzzy STONE_BRICK = new Fuzzy(ItemTags.STONE_BRICKS, "brickStone");
    public static final Fuzzy SAND = new Fuzzy(ItemTags.SAND, "sand");
    public static final Fuzzy PISTON = new Fuzzy("piston").add(Blocks.PISTON);

    public static final Fuzzy GOLD_INGOT = new Fuzzy(getTag("c:gold_ingots"), "ingotGold").add(Items.GOLD_INGOT);
    public static final Fuzzy STEEL_INGOT = new Fuzzy(getTag("c:steel_ingots"), "ingotSteel");
    public static final Fuzzy STEEL_BLOCK = new Fuzzy(getTag("c:steel_blocks"), "blockSteel");
    public static final Fuzzy IRON_INGOT = new Fuzzy(getTag("c:iron_ingots"), "ingotIron").add(Items.IRON_INGOT);
    public static final Fuzzy IRON_BLOCK = new Fuzzy(getTag("c:iron_blocks"), "blockIron").add(Items.IRON_BLOCK);
    public static final Fuzzy IRON_BARS = new Fuzzy("barsIron").add(Blocks.IRON_BARS);

    public static final Fuzzy NETHER_BRICK = new Fuzzy("brickNether").add(Blocks.NETHER_BRICKS);
    public static final Fuzzy GRAVEL_BLOCK = new Fuzzy("gravel").add(Blocks.GRAVEL);
    public static final Fuzzy BRICK_BLOCK = new Fuzzy("brickBlock").add(Blocks.BRICKS);
    public static final Fuzzy COBBLESTONE = new Fuzzy(getTag("c:cobblestone"), "cobblestone").add(Blocks.COBBLESTONE);
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


    public static final Fuzzy DIRT = new Fuzzy(getTag("c:dirt"), "dirt").add(Blocks.DIRT);
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

    public static final Fuzzy LOG_WOOD = new Fuzzy(ItemTags.LOGS, "logWood");
    public static final Fuzzy PAPER = new Fuzzy("paper").add(Items.PAPER);
    public static final Fuzzy BOOK = new Fuzzy("book").add(Items.BOOK);
    public static final Fuzzy WOOL_BLOCK = new Fuzzy(ItemTags.WOOL, "wool");
    public static final Fuzzy BUCKET = new Fuzzy("bucket").add(Items.BUCKET);
    public static final Fuzzy EMERALD = new Fuzzy(getTag("c:emeralds"), "gemEmerald").add(Items.EMERALD);
    public static final Fuzzy REDSTONE_TORCH = new Fuzzy("redstoneTorch").add(Blocks.REDSTONE_TORCH);
    public static final Fuzzy GLASS_PANE = new Fuzzy(getTag("c:glass_panes"), "paneGlass");

    static {
        ConfigFile.addMapper(Fuzzy.class, Fuzzy::toString, Fuzzy::get);
    }

    static Map<String, Fuzzy> registered;
    private final String ident;
    final Tag<Item> tag;
    private List<Item> customItems = new ArrayList<>();
    private final Set<Fuzzy> includes;

    public static Fuzzy get(String ident) {
        if (registered == null) {
            registered = new HashMap<>();
        }
        if (!registered.containsKey(ident)) {
            registered.put(ident, new Fuzzy(ident));
        }
        return registered.get(ident);
    }

    /**
     * Create fuzzy with this name
     */
    private Fuzzy(String ident) {
        this(
                getTag(ident.contains(":") ? new Identifier(ident.toLowerCase()).toString() :
                        new Identifier("c", ident.toLowerCase()).toString()),
                ident
        );
    }

    public Fuzzy(Tag<Item> tag, String ident) {
        if (registered == null) {
            registered = new HashMap<>();
        }

        this.ident = ident;
        this.tag = tag;
        this.customItems = new ArrayList<>();
        includes = new HashSet<>();
        registered.put(ident, this);
    }

    /**
     * Is the item in this stack matched by this fuzzy?
     */
    public boolean matches(ItemStack stack) {
        return enumerate().stream().anyMatch(potential -> potential.internal.getItem() == stack.internal.getItem());
    }

    /**
     * Do any items exist in this fuzzy
     */
    public boolean isEmpty() {
        return enumerate().isEmpty();
    }

    /**
     * List all possible itemstacks
     */
    public List<ItemStack> enumerate() {
        List<ItemStack> items = tag.values().stream().map(item -> new ItemStack(new net.minecraft.item.ItemStack(item))).collect(Collectors.toList());
        customItems.forEach(item -> items.add(new ItemStack(new net.minecraft.item.ItemStack(item))));
        return items;
    }

    /**
     * Grab the first example of a item in this fuzzy
     */
    public ItemStack example() {
        List<ItemStack> stacks = enumerate();
        return stacks.size() != 0 ? stacks.get(0) : ItemStack.EMPTY;
    }

    /**
     * Use to register an itemstack
     */
    public Fuzzy add(ItemStack item) {
        add(item.internal.getItem());
        return this;
    }

    /**
     * Don't use directly (unless in version specific code)
     */
    public Fuzzy add(Block block) {
        add(block.asItem());
        return this;
    }

    /**
     * Don't use directly (unless in version specific code)
     */
    public Fuzzy add(Item item) {
        customItems.add(item);
        return this;
    }

    /** Use to register an item */
    public Fuzzy add (CustomItem item){
        return add(item.internal);
    }

    /** Pull other fuzzy into this one */
    public Fuzzy include (Fuzzy other){
        includes.add(other);
        return this;
    }

    @Override
    public String toString () {
        return ident;
    }
}
