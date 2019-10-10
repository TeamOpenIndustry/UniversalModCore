package cam72cam.mod.item;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.registry.Registry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBase {
    private static final String CUSTOM_NAME_KEY = "universalmodcore_name";
    public final Item internal;
    private final CreativeTab[] creativeTabs;
    private final Identifier identifier;

    public ItemBase(String modID, String name, int stackSize, CreativeTab... tabs) {
        internal = new ItemInternal(modID, name, stackSize, tabs);
        identifier = new Identifier(modID, name);
        this.creativeTabs = tabs;

        Registry.ITEM.add(identifier.internal, internal);

        for (CreativeTab tab : tabs) {
            tab.items.add(list -> list.addAll(getItemVariants(tab).stream().map(x -> x.internal).collect(Collectors.toList())));
        }
    }

    public List<ItemStack> getItemVariants(CreativeTab creativeTab) {
        List<ItemStack> res = new ArrayList<>();
        if (creativeTab == null || creativeTab.internal == internal.getGroup()) {
            res.add(new ItemStack(internal, 1));
        }
        return res;
    }

    /* Overrides */

    public void addInformation(ItemStack itemStack, List<String> tooltip) {
    }

    public ClickResult onClickBlock(Player player, World world, Vec3i vec3i, Hand from, Facing from1, Vec3d vec3d) {
        return ClickResult.PASS;
    }

    public void onClickAir(Player player, World world, Hand hand) {

    }

    public boolean isValidArmor(ItemStack itemStack, ArmorSlot from, Entity entity) {
        return false;//internal.isValidArmor(itemStack.internal, from.internal, entity.internal);
    }

    public String getCustomName(ItemStack stack) {
        return null;
    }

    /* Name Hacks */

    protected final void applyCustomName(ItemStack stack) {
        String custom = getCustomName(stack);
        if (custom != null) {
            stack.getTagCompound().setString(CUSTOM_NAME_KEY, custom);
        }
    }

    public Identifier getRegistryName() {
        return identifier;
    }

    private class ItemInternal extends Item {
        public ItemInternal(String modID, String name, int stackSize, CreativeTab[] tabs) {
            super(new Item.Settings().group(tabs[0].internal).maxCount(stackSize));
            // TODO 1.14.4 MobEntity.canEquipmentSlotContain()
        }

        @Override
        public Text getName(net.minecraft.item.ItemStack stack) {
            if (stack.getTag() != null && stack.getTag().containsKey(CUSTOM_NAME_KEY)) {
                return new LiteralText(stack.getTag().getString(CUSTOM_NAME_KEY));
            }
            return super.getName(stack);
        }

        @Override
        @Environment(EnvType.CLIENT)
        public void appendTooltip(net.minecraft.item.ItemStack stack, @Nullable net.minecraft.world.World worldIn, List<Text> tooltip, TooltipContext context) {
            super.appendTooltip(stack, worldIn, tooltip, context);
            List<String> temp = new ArrayList<>();
            try {
                ItemBase.this.addInformation(new ItemStack(stack), temp);
            } catch (Exception ex) {
                //TODO
            }
            temp.forEach(x -> tooltip.add(new LiteralText(x)));
        }

        @Override
        public ActionResult useOnBlock(ItemUsageContext context) {
            return ItemBase.this.onClickBlock(new Player(context.getPlayer()), World.get(context.getWorld()), new Vec3i(context.getBlockPos()), Hand.from(context.getHand()), Facing.from(context.getSide()), new Vec3d(context.getHitPos())).internal;
        }

        @Override
        public TypedActionResult<net.minecraft.item.ItemStack> use(net.minecraft.world.World world, PlayerEntity player, net.minecraft.util.Hand hand) {
            onClickAir(new Player(player), World.get(world), Hand.from(hand));
            return super.use(world, player, hand);
        }
    }
}
