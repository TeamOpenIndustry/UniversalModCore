package cam72cam.mod.item;

import cam72cam.mod.ModCore;
import cam72cam.mod.config.ConfigFile;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.*;
import java.util.stream.Collectors;

/** OreDict / Tag abstraction.  Use for item equivalence */
public class Fuzzy {
    public static final Fuzzy WOOD_STICK = new Fuzzy(Tags.Items.RODS_WOODEN, "stickWood");
    public static final Fuzzy WOOD_PLANK = new Fuzzy(ItemTags.PLANKS, "plankWood");
    public static final Fuzzy REDSTONE_DUST = new Fuzzy(Tags.Items.DUSTS_REDSTONE, "dustRedstone");
    public static final Fuzzy SNOW_LAYER = new Fuzzy("layerSnow").add(Blocks.SNOW);
    public static final Fuzzy SNOW_BLOCK = new Fuzzy("blockSnow").add(Blocks.SNOW_BLOCK);
    public static final Fuzzy LEAD = new Fuzzy("lead").add(Items.LEAD);

    public static final Fuzzy STONE_SLAB = new Fuzzy("slabStone").add(Items.STONE_SLAB);
    public static final Fuzzy STONE_BRICK = new Fuzzy(ItemTags.STONE_BRICKS, "brickStone");
    public static final Fuzzy SAND = new Fuzzy(Tags.Items.SAND, "sand");
    public static final Fuzzy PISTON = new Fuzzy("piston").add(Items.PISTON);

    public static final Fuzzy GOLD_INGOT = new Fuzzy(Tags.Items.INGOTS_GOLD, "ingotGold");
    public static final Fuzzy STEEL_INGOT = new Fuzzy(ItemTags.bind(new ResourceLocation("forge", "ingots/steel").toString()), "ingotSteel");
    public static final Fuzzy STEEL_BLOCK = new Fuzzy(ItemTags.bind(new ResourceLocation("forge", "storage_blocks/steel").toString()), "blockSteel");
    public static final Fuzzy IRON_INGOT = new Fuzzy(Tags.Items.INGOTS_IRON, "ingotIron");
    public static final Fuzzy IRON_BLOCK = new Fuzzy(Tags.Items.STORAGE_BLOCKS_IRON, "blockIron");
    public static final Fuzzy IRON_BARS = new Fuzzy("barsIron").add(Blocks.IRON_BARS);

    public static final Fuzzy NETHER_BRICK = new Fuzzy("brickNether").add(Blocks.NETHER_BRICKS);
    public static final Fuzzy GRAVEL_BLOCK = new Fuzzy(Tags.Items.GRAVEL, "gravel");
    public static final Fuzzy BRICK_BLOCK = new Fuzzy("brickBlock").add(Blocks.BRICKS);
    public static final Fuzzy COBBLESTONE = new Fuzzy(Tags.Items.COBBLESTONE, "cobblestone");
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


    public static final Fuzzy DIRT = new Fuzzy( "dirt").add(Items.DIRT);
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
    public static final Fuzzy EMERALD = new Fuzzy(Tags.Items.GEMS_EMERALD, "gemEmerald");
    public static final Fuzzy REDSTONE_TORCH = new Fuzzy("redstoneTorch").add(Blocks.REDSTONE_TORCH);
    public static final Fuzzy GLASS_PANE = new Fuzzy(Tags.Items.GLASS_PANES, "paneGlass");

    static {
        ConfigFile.addMapper(Fuzzy.class, Fuzzy::toString, Fuzzy::get);
    }

    static Map<String, Fuzzy> registered;
    private final String ident;
    final ITag.INamedTag<Item> tag;
    private final List<Item> customItems;
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


    /** Create fuzzy with this name */
    private Fuzzy(String ident) {
        this(ItemTags.createOptional(
                ident.contains(":") ? new ResourceLocation(ident.toLowerCase(Locale.ROOT)) :
                        new ResourceLocation("forge", ident.toLowerCase(Locale.ROOT))
        ), ident);
    }

    private Fuzzy(ITag.INamedTag<Item> tag, String ident) {
        if (registered == null) {
            registered = new HashMap<>();
        }

        this.ident = ident;
        this.tag = tag;
        this.customItems = new ArrayList<>();
        includes = new HashSet<>();
        registered.put(ident, this);
    }

    /** Is the item in this stack matched by this fuzzy? */
    public boolean matches(ItemStack stack) {
        return enumerate().stream().anyMatch(potential -> potential.internal.getItem() == stack.internal.getItem());
    }

    /** Do any items exist in this fuzzy */
    public boolean isEmpty() {
        return enumerate().isEmpty();
    }

    /** List all possible itemstacks */
    public List<ItemStack> enumerate() {
        Set<ItemStack> items;
        try {
            items = tag.getValues().stream().map(item -> new ItemStack(new net.minecraft.item.ItemStack(item))).collect(Collectors.toSet());
        } catch (IllegalStateException e) {
            ModCore.warn("Unsafe tag access before load, try to avoid this if possible");
            items = new HashSet<>();
        }
        for (Item item : customItems) {
            items.add(new ItemStack(new net.minecraft.item.ItemStack(item)));
        }
        for (Fuzzy f : includes) {
            items.addAll(f.enumerate());
        }
        return new ArrayList<>(items);
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
    public Fuzzy add(CustomItem item) {
        return add(item.internal);
    }

    /** Pull other fuzzy into this one */
    public Fuzzy include(Fuzzy other) {
        includes.add(other);
        return this;
    }

    @Override
    public String toString() {
        return ident;
    }

    public static void register(DataGenerator gen, ExistingFileHelper existingFileHelper) {
        BlockTagsProvider blocktagsprovider = new BlockTagsProvider(gen, ModCore.MODID, existingFileHelper) {
            @Override
            protected void addTags() {
                //super.addTags();
            }
        };
        gen.addProvider(blocktagsprovider);
        gen.addProvider(new ItemTagsProvider(gen,blocktagsprovider, ModCore.MODID, existingFileHelper) {
            @Override
            protected void addTags() {
                for (Fuzzy value : registered.values()) {
                    //if (!value.customItems.isEmpty() || !value.includes.isEmpty()) {
                        Builder<Item> builder = tag(value.tag);
                        for (Item customItem : value.customItems) {
                            builder.add(customItem);
                        }
                        for (Fuzzy include : value.includes) {
                            builder.addTag(include.tag);
                        }
                    //}
                }
            }
        });
    }
}
