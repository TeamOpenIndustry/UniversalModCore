package cam72cam.mod.item;

import cam72cam.mod.config.ConfigFile;
import cam72cam.mod.event.CommonEvents;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;
import java.util.stream.Collectors;

/** OreDict / Tag abstraction.  Use for item equivalence */
public class Fuzzy {
    public static final Fuzzy WOOD_STICK = new Fuzzy("stickWood");
    public static final Fuzzy WOOD_PLANK = new Fuzzy("plankWood");
    public static final Fuzzy REDSTONE_DUST = new Fuzzy("dustRedstone");
    public static final Fuzzy SNOW_LAYER = new Fuzzy("layerSnow").add(Blocks.SNOW_LAYER);
    public static final Fuzzy SNOW_BLOCK = new Fuzzy("blockSnow").add(Blocks.SNOW);
    public static final Fuzzy LEAD = new Fuzzy("lead").add(Items.LEAD);

    public static final Fuzzy STONE_SLAB = new Fuzzy("slabStone").add(Blocks.STONE_SLAB);
    public static final Fuzzy STONE_BRICK = new Fuzzy("brickStone").add(Blocks.STONEBRICK);
    public static final Fuzzy SAND = new Fuzzy("sand");
    public static final Fuzzy PISTON = new Fuzzy("piston").add(Blocks.PISTON);

    public static final Fuzzy GOLD_INGOT = new Fuzzy("ingotGold");
    public static final Fuzzy STEEL_INGOT = new Fuzzy("ingotSteel");
    public static final Fuzzy STEEL_BLOCK = new Fuzzy("blockSteel");
    public static final Fuzzy IRON_INGOT = new Fuzzy("ingotIron");
    public static final Fuzzy IRON_BLOCK = new Fuzzy("blockIron");
    public static final Fuzzy IRON_BARS = new Fuzzy("barsIron").add(Blocks.IRON_BARS);

    public static final Fuzzy NETHER_BRICK = new Fuzzy("brickNether").add(Blocks.NETHER_BRICK);
    public static final Fuzzy GRAVEL_BLOCK = new Fuzzy("gravel");
    public static final Fuzzy BRICK_BLOCK = new Fuzzy("brickBlock");
    public static final Fuzzy COBBLESTONE = new Fuzzy("cobblestone");
    public static final Fuzzy CONCRETE = new Fuzzy("concrete").add(new ItemStack(new net.minecraft.item.ItemStack(Blocks.HARDENED_CLAY, 1, OreDictionary.WILDCARD_VALUE)));
    public static final Fuzzy DIRT = new Fuzzy("dirt");
    public static final Fuzzy HARDENED_CLAY = new Fuzzy("hardened_clay").add(new ItemStack(new net.minecraft.item.ItemStack(Blocks.HARDENED_CLAY, 1, OreDictionary.WILDCARD_VALUE)));
    public static final Fuzzy LOG_WOOD = new Fuzzy("logWood");
    public static final Fuzzy PAPER = new Fuzzy("paper");
    public static final Fuzzy BOOK = new Fuzzy("book").add(Items.BOOK);
    public static final Fuzzy WOOL_BLOCK = new Fuzzy("wool").add(new ItemStack(new net.minecraft.item.ItemStack(Blocks.WOOL, 1, OreDictionary.WILDCARD_VALUE)));
    public static final Fuzzy BUCKET = new Fuzzy("bucket").add(new ItemStack(new net.minecraft.item.ItemStack(Items.BUCKET, 1)));
    public static final Fuzzy EMERALD = new Fuzzy("gemEmerald");
    public static final Fuzzy REDSTONE_TORCH = new Fuzzy("redstoneTorch").add(Blocks.REDSTONE_TORCH);
    public static final Fuzzy GLASS_PANE = new Fuzzy("paneGlass");

    private static boolean isPostItemRegistration = false;

    /** Setup ore dict *after* items have been registered */
    private static void postItemRegistration(Runnable fn) {
        if(isPostItemRegistration) {
            fn.run();
        } else {
            CommonEvents.Item.REGISTER.post(fn);
        }
    }

    static {
        CommonEvents.Item.REGISTER.post(() -> isPostItemRegistration = true);
        ConfigFile.addMapper(Fuzzy.class, Fuzzy::toString, Fuzzy::get);
    }

    private final String ident;
    private final Set<Fuzzy> includes;

    private static Map<String, Fuzzy> registered;

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
        if (registered == null) {
            registered = new HashMap<>();
        }
        this.ident = ident;
        this.includes = new HashSet<>();
    }

    /** Is the item in this stack matched by this fuzzy? */
    public boolean matches(ItemStack stack) {
        return OreDictionary.getOres(ident).stream()
                        .anyMatch((net.minecraft.item.ItemStack potential) -> OreDictionary.itemMatches(potential, stack.internal, false)) ||
                        includes.stream().anyMatch(f -> f.matches(stack));
    }

    /** Do any items exist in this fuzzy */
    public boolean isEmpty() {
        return enumerate().isEmpty();
    }

    /** List all possible itemstacks */
    public List<ItemStack> enumerate() {
        List<ItemStack> results = new ArrayList<>();
        for (net.minecraft.item.ItemStack stack : OreDictionary.getOres(ident)) {
            if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                NonNullList<net.minecraft.item.ItemStack> temp = NonNullList.create();
                try {
                    stack.getItem().getSubItems(stack.getItem(), stack.getItem().getCreativeTab(), temp);
                } catch (NoSuchMethodError e) {
                    // This function does not exist serverside, FML
                    temp.add(new net.minecraft.item.ItemStack(stack.getItem()));
                }
                results.addAll(temp.stream().map(ItemStack::new).collect(Collectors.toList()));
            } else {
                results.add(new ItemStack(stack));
            }
        }
        for (Fuzzy include : includes) {
            results.addAll(include.enumerate());
        }
        return results;
    }

    /** Grab the first example of a item in this fuzzy */
    public ItemStack example() {
        List<ItemStack> stacks = enumerate();
        return stacks.size() != 0 ? stacks.get(0) : ItemStack.EMPTY;
    }

    /** Use to register an itemstack */
    public Fuzzy add(ItemStack item) {
        postItemRegistration(() -> OreDictionary.registerOre(ident, item.internal));
        return this;
    }

    /** Don't use directly (unless in version specific code) */
    public Fuzzy add(Block block) {
        postItemRegistration(() -> OreDictionary.registerOre(ident, block));
        return this;
    }

    /** Don't use directly (unless in version specific code) */
    public Fuzzy add(Item item) {
        postItemRegistration(() -> OreDictionary.registerOre(ident, item));
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
}
