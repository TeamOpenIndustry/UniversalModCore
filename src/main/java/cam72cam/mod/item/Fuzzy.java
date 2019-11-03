package cam72cam.mod.item;

import cam72cam.mod.config.ConfigFile;
import com.google.gson.JsonArray;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Fuzzy {
    static Map<String, Fuzzy> tags = new HashMap<>();

    public static final Fuzzy WOOD_STICK = new Fuzzy("stickWood").add(Items.STICK);
    public static final Fuzzy WOOD_PLANK = new Fuzzy("plankWood", ItemTags.PLANKS);
    public static final Fuzzy REDSTONE_DUST = new Fuzzy("dustRedstone").add(Items.REDSTONE);
    public static final Fuzzy SNOW_LAYER = new Fuzzy("layerSnow").add(Blocks.SNOW);
    public static final Fuzzy SNOW_BLOCK = new Fuzzy("blockSnow").add(Blocks.SNOW_BLOCK);
    public static final Fuzzy LEAD = new Fuzzy("lead").add(Items.LEAD);

    public static final Fuzzy STONE_SLAB = new Fuzzy("slabStone", ItemTags.SLABS);
    public static final Fuzzy STONE_BRICK = new Fuzzy("brickStone", ItemTags.STONE_BRICKS);
    public static final Fuzzy SAND = new Fuzzy("sand", ItemTags.SAND);
    public static final Fuzzy PISTON = new Fuzzy("piston").add(Blocks.PISTON);

    public static final Fuzzy GOLD_INGOT = new Fuzzy("ingotGold").add(Items.GOLD_INGOT);
    public static final Fuzzy STEEL_INGOT = new Fuzzy("ingotSteel");
    public static final Fuzzy STEEL_BLOCK = new Fuzzy("blockSteel");
    public static final Fuzzy IRON_INGOT = new Fuzzy("ingotIron").add(Items.IRON_INGOT);
    public static final Fuzzy IRON_BLOCK = new Fuzzy("blockIron").add(Items.IRON_BLOCK);
    public static final Fuzzy IRON_BARS = new Fuzzy("barsIron").add(Blocks.IRON_BARS);

    public static final Fuzzy NETHER_BRICK = new Fuzzy("brickNether").add(Blocks.NETHER_BRICKS);
    public static final Fuzzy GRAVEL_BLOCK = new Fuzzy("gravel").add(Blocks.GRAVEL);
    public static final Fuzzy BRICK_BLOCK = new Fuzzy("brickBlock").add(Blocks.BRICKS);
    public static final Fuzzy COBBLESTONE = new Fuzzy("cobblestone").add(Blocks.COBBLESTONE);
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


    public static final Fuzzy DIRT = new Fuzzy("dirt").add(Blocks.DIRT);
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

    public static final Fuzzy LOG_WOOD = new Fuzzy("logWood", ItemTags.LOGS);
    public static final Fuzzy PAPER = new Fuzzy("paper").add(Items.PAPER);
    public static final Fuzzy BOOK = new Fuzzy("book").add(Items.BOOK);
    public static final Fuzzy WOOL_BLOCK = new Fuzzy("wool", ItemTags.WOOL);
    public static final Fuzzy BUCKET = new Fuzzy("bucket").add(Items.BUCKET);
    public static final Fuzzy EMERALD = new Fuzzy("gemEmerald").add(Items.EMERALD);
    public static final Fuzzy REDSTONE_TORCH = new Fuzzy("redstoneTorch").add(Blocks.REDSTONE_TORCH);
    public static final Fuzzy GLASS_PANE = new Fuzzy("paneGlass");

    static {
        ConfigFile.addMapper(Fuzzy.class, Fuzzy::toString, Fuzzy::new);
    }

    private final String ident;
    private final Tag<Item> tag;
    private List<Tag<Item>> subTags = new ArrayList<>();

    public Fuzzy(String ident) {
        this.ident = ident;
        if (tags.containsKey(ident)) {
            // Clone
            this.tag = tags.get(ident).tag;
            this.subTags = tags.get(ident).subTags;
        } else {
            // New
            Tag<Item> currentTag = ItemTags.getContainer().get(new Identifier(ident));
            if (currentTag != null) {
                this.tag = currentTag;
            } else {
                // I don't think this will survive a reload...
                this.tag = new Tag<>(new Identifier(ident.toLowerCase()), new ArrayList<>(), true);
                ItemTags.getContainer().getEntries().put(new Identifier(ident), this.tag);
            }

            this.subTags.add(tag);
            tags.put(ident, this);
        }
    }

    private Fuzzy(String ident, Tag<Item> tag) {
        // alias
        this.ident = ident;
        this.tag = tag;
        tags.put(ident, this);
        subTags.add(tag);
    }


    private List<Item> values() {
        return subTags.stream().map(Tag::values).collect(ArrayList::new, List::addAll, List::addAll);
    }

    public boolean matches(ItemStack stack) {
        return values().stream().anyMatch(potential -> potential == stack.internal.getItem());
    }

    public List<ItemStack> enumerate() {
        return values().stream().map(ItemStack::new).collect(Collectors.toList());
    }

    public ItemStack example() {
        List<ItemStack> stacks = enumerate();
        return stacks.size() != 0 ? stacks.get(0) : ItemStack.EMPTY;
    }

    public Fuzzy add(ItemStack item) {
        add(item.internal.getItem());
        return this;
    }

    public Fuzzy add(Block block) {
        add(block.asItem());
        return this;
    }

    public Fuzzy add(Item item) {
        tag.entries().add(new Tag.Entry<Item>() {
            @Override
            public void build(Collection<Item> var1) {
                var1.add(item);
            }

            @Override
            public void toJson(JsonArray var1, Function<Item, Identifier> var2) {
                // NOP
            }
        });
        tag.values().add(item);
        return this;
    }

    public void add(ItemBase item) {
        add(item.internal);
    }

    public Fuzzy addAll(Fuzzy other) {
        subTags.add(other.tag);
        return this;
    }

    public void clear() {
        // This might break stuff in fantastic ways!
        tag.entries().clear();
    }

    @Override
    public String toString() {
        return ident;
    }
}
