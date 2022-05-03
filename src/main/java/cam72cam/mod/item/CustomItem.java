package cam72cam.mod.item;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Implement to create/register a custom item */
public abstract class CustomItem {
    public final Item internal;
    public CustomItem(String modID, String name) {
        internal = new ItemInternal();
        internal.setUnlocalizedName(modID + ":" + name);
        internal.setMaxStackSize(getStackSize());
        if (!getCreativeTabs().isEmpty()) {
            internal.setCreativeTab(getCreativeTabs().get(0).internal);
        }

        CommonEvents.Item.REGISTER.subscribe(() -> GameRegistry.registerItem(internal, name, modID));
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
        return new Identifier(internal.getUnlocalizedName());
    }

    // Removed 1.7.10 @Optional.Interface(iface = "mezz.jei.api.ingredients.ISlowRenderItem", modid = "jei")
    private class ItemInternal extends Item {
        @Override
        public String getItemStackDisplayName(net.minecraft.item.ItemStack stack) {
            String custom = getCustomName(new ItemStack(stack));
            if (custom != null) {
                return custom;
            }
            return super.getItemStackDisplayName(stack);
        }

        public void getSubItems(Item itemIn, CreativeTabs tab, List items) {
            CreativeTab myTab = tab != CreativeTabs.tabAllSearch && tab != null ? new CreativeTab(tab) : null;
            items.addAll(getItemVariants(myTab).stream().map((ItemStack stack) -> stack.internal).collect(Collectors.toList()));
        }

        @Override
        @SideOnly(Side.CLIENT)
        public final void addInformation(net.minecraft.item.ItemStack stack, EntityPlayer entityPlayer, List tooltip, boolean flagIn) {
            super.addInformation(stack, entityPlayer, tooltip, flagIn);
            tooltip.addAll(CustomItem.this.getTooltip(new ItemStack(stack)));
        }

        @Override
        public final boolean onItemUse(net.minecraft.item.ItemStack stack, EntityPlayer player, net.minecraft.world.World worldIn, int posX, int posY, int posZ, int facing, float hitX, float hitY, float hitZ) {
            return CustomItem.this.onClickBlock(new Player(player), World.get(worldIn), new Vec3i(posX, posY, posZ), Player.Hand.PRIMARY, Facing.from((byte)facing), new Vec3d(hitX, hitY, hitZ)).internal;
        }

        @Override
        public final net.minecraft.item.ItemStack onItemRightClick(net.minecraft.item.ItemStack stack, net.minecraft.world.World world, EntityPlayer player) {
            onClickAir(new Player(player), World.get(world), Player.Hand.PRIMARY);
            return super.onItemRightClick(stack, world, player);
        }

        @Override
        public final boolean isValidArmor(net.minecraft.item.ItemStack stack, int armorType, net.minecraft.entity.Entity entity) {
            return CustomItem.this.isValidArmor(new ItemStack(stack), ArmorSlot.from(armorType), new Entity(entity));
        }

        @Override
        public final CreativeTabs[] getCreativeTabs() {
            return CustomItem.this.getCreativeTabs().stream().map((CreativeTab tab) -> tab.internal).toArray(CreativeTabs[]::new);
        }

        /*
        @SideOnly(Side.CLIENT)
        public IIcon getIconFromDamageForRenderPass(int p_77618_1_, int p_77618_2_)
        {
            return ItemRender.getIcon(ItemBase.this);
        }

        @SideOnly(Side.CLIENT)
        public IIcon getIconFromDamage(int p_77617_1_) {
            return ItemRender.getIcon(ItemBase.this);
        }
        */

        @SideOnly(Side.CLIENT)
        public void registerIcons(IIconRegister ir) {
            String iconName = ItemRender.getIcon(CustomItem.this);
            if (iconName != null) {
                this.itemIcon = ir.registerIcon(iconName.replace(":items/", ":"));
            } else {
                super.registerIcons(ir);
            }
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
