package cam72cam.mod.item;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.registry.Registry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Implement to create/register a custom item */
public abstract class CustomItem {
    public final Item internal;
    private final Identifier identifier;

    public CustomItem(String modID, String name) {
        internal = new ItemInternal();
        identifier = new Identifier(modID, name);

        Registry.ITEM.add(identifier.internal, internal);

        for (CreativeTab tab : getCreativeTabs()) {
            tab.items.add(list -> list.addAll(getItemVariants(tab).stream().map(x -> x.internal).collect(Collectors.toList())));
        }
    }

    /** Creative tabs that this should be shown under */
    public abstract List<CreativeTab> getCreativeTabs();

    /** Max stack size for this item */
    public int getStackSize() {
        return 64;
    }

    /** Return variants of this itemstack to add to this particular creative tab */
    public List<ItemStack> getItemVariants(CreativeTab creativeTab) {
        List<ItemStack> res = new ArrayList<>();
        if (creativeTab == null || creativeTab.internal == internal.getGroup()) {
            res.add(new ItemStack(new net.minecraft.item.ItemStack(internal, 1)));
        }
        return res;
    }

    /** Provide custom tooltips (client side only) */
    public List<String> getTooltip(ItemStack itemStack) {
        return Collections.emptyList();
    }

    /** Called when the item is used to click on a block */
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d inBlockPos) {
        return ClickResult.PASS;
    }

    /** Called when the item is used to click on nothing */
    public void onClickAir(Player player, World world, Player.Hand hand) {

    }

    /** If the item can be used as armor (Warning: Partial Support!) */
    public boolean isValidArmor(ItemStack itemStack, ArmorSlot from, Entity entity) {
        return false;
    }

    /** Allows you to override the name of a given itemstack */
    public String getCustomName(ItemStack stack) {
        return null;
    }

    /** Identifier of this item */
    public Identifier getRegistryName() {
        return identifier;
    }

    private class ItemInternal extends Item {
        public ItemInternal() {
            super(new Item.Settings().group(getCreativeTabs().get(0).internal).maxCount(getStackSize()));
            // TODO 1.14.4 MobEntity.canEquipmentSlotContain()
        }

        @Override
        public void appendStacks(ItemGroup tab, DefaultedList<net.minecraft.item.ItemStack> items) {
            CreativeTab myTab = tab != null && tab != ItemGroup.SEARCH ? new CreativeTab(tab) : null;
            items.addAll(getItemVariants(myTab).stream().map((ItemStack stack) -> stack.internal).collect(Collectors.toList()));
        }

        @Override
        public Text getName(net.minecraft.item.ItemStack stack) {
            String custom = getCustomName(new ItemStack(stack));
            if (custom != null) {
                return new LiteralText(custom);
            }
            return super.getName(stack);
        }

        @Override
        @Environment(EnvType.CLIENT)
        public void appendTooltip(net.minecraft.item.ItemStack stack, @Nullable net.minecraft.world.World worldIn, List<Text> tooltip, TooltipContext context) {
            super.appendTooltip(stack, worldIn, tooltip, context);
            tooltip.addAll(CustomItem.this.getTooltip(new ItemStack(stack)).stream().map(LiteralText::new).collect(Collectors.toList()));
        }

        @Override
        public ActionResult useOnBlock(ItemUsageContext context) {
            return CustomItem.this.onClickBlock(new Player(context.getPlayer()), World.get(context.getWorld()), new Vec3i(context.getBlockPos()), Player.Hand.from(context.getHand()), Facing.from(context.getSide()), new Vec3d(context.getHitPos()).subtract(new Vec3i(context.getBlockPos()))).internal;
        }

        @Override
        public TypedActionResult<net.minecraft.item.ItemStack> use(net.minecraft.world.World world, PlayerEntity player, net.minecraft.util.Hand hand) {
            onClickAir(new Player(player), World.get(world), Player.Hand.from(hand));
            return super.use(world, player, hand);
        }
    }
    /**
     * Helper for serializing / deserializing data on a stack
     *
     * Example:
     * <pre>
     * {@code
     * public class Data extends ItemDataSerailizer {
     *     (at)TagField
     *     public int myField
     * }
     * }
     * </pre>
     */
    public abstract static class ItemDataSerializer {
        private final ItemStack stack;

        protected ItemDataSerializer(ItemStack stack) {
            this.stack = stack;
            try {
                TagSerializer.deserialize(stack.getTagCompound(), this);
            } catch (SerializationException e) {
                ModCore.catching(e);
            }
        }

        public void write() {
            try {
                TagSerializer.serialize(stack.getTagCompound(), this);
            } catch (SerializationException e) {
                ModCore.catching(e);
            }
        }
    }
}
