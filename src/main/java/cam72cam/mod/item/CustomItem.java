package cam72cam.mod.item;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Implement to create/register a custom item */
public abstract class CustomItem {
    public final Item internal;
    public CustomItem(String modID, String name) {
        internal = new ItemInternal();
        internal.setTranslationKey(modID + ":" + name);
        internal.setRegistryName(new ResourceLocation(modID, name));
        internal.setMaxStackSize(getStackSize());
        internal.setCreativeTab(getCreativeTabs().get(0).internal);

        CommonEvents.Item.REGISTER.subscribe(() -> ForgeRegistries.ITEMS.register(internal));
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
        if (creativeTab == null || creativeTab.internal == internal.getCreativeTab()) {
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
    public final Identifier getRegistryName() {
        return new Identifier(internal.getRegistryName());
    }

    @Optional.Interface(iface = "mezz.jei.api.ingredients.ISlowRenderItem", modid = "jei")
    private class ItemInternal extends Item {
        @Override
        public String getItemStackDisplayName(net.minecraft.item.ItemStack stack) {
            String custom = getCustomName(new ItemStack(stack));
            if (custom != null) {
                return custom;
            }
            return super.getItemStackDisplayName(stack);
        }

        @Override
        public final void getSubItems(CreativeTabs tab, NonNullList<net.minecraft.item.ItemStack> items) {
            CreativeTab myTab = tab != CreativeTabs.SEARCH ? new CreativeTab(tab) : null;
            items.addAll(getItemVariants(myTab).stream().map((ItemStack stack) -> stack.internal).collect(Collectors.toList()));
        }

        @Override
        @SideOnly(Side.CLIENT)
        public final void addInformation(net.minecraft.item.ItemStack stack, @Nullable net.minecraft.world.World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
            super.addInformation(stack, worldIn, tooltip, flagIn);
            tooltip.addAll(CustomItem.this.getTooltip(new ItemStack(stack)));
        }

        @Override
        public final EnumActionResult onItemUse(EntityPlayer player, net.minecraft.world.World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
            return CustomItem.this.onClickBlock(new Player(player), World.get(worldIn), new Vec3i(pos), Player.Hand.from(hand), Facing.from(facing), new Vec3d(hitX, hitY, hitZ)).internal;
        }

        @Override
        public final ActionResult<net.minecraft.item.ItemStack> onItemRightClick(net.minecraft.world.World world, EntityPlayer player, EnumHand hand) {
            onClickAir(new Player(player), World.get(world), Player.Hand.from(hand));
            return super.onItemRightClick(world, player, hand);
        }

        @Override
        public final boolean isValidArmor(net.minecraft.item.ItemStack stack, EntityEquipmentSlot armorType, net.minecraft.entity.Entity entity) {
            return CustomItem.this.isValidArmor(new ItemStack(stack), ArmorSlot.from(armorType), World.get(entity.world).getEntity(entity));
        }

        @Override
        public final CreativeTabs[] getCreativeTabs() {
            return CustomItem.this.getCreativeTabs().stream().map((CreativeTab tab) -> tab.internal).toArray(CreativeTabs[]::new);
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
