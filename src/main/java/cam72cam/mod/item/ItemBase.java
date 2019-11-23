package cam72cam.mod.item;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;
import com.mojang.realmsclient.gui.ChatFormatting;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.IIcon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBase {
    public final Item internal;
    private final CreativeTab[] creativeTabs;
    private final String modID;
    private final String name;

    public ItemBase(String modID, String name, int stackSize, CreativeTab... tabs) {
        internal = new ItemInternal();
        internal.setUnlocalizedName(modID + ":" + name);
        internal.setMaxStackSize(stackSize);
        internal.setCreativeTab(tabs[0].internal);
        this.creativeTabs = tabs;
        this.modID = modID;
        this.name = name;

        CommonEvents.Item.REGISTER.subscribe(() -> GameRegistry.registerItem(internal, name, modID));
    }

    public List<ItemStack> getItemVariants(CreativeTab creativeTab) {
        List<ItemStack> res = new ArrayList<>();
        if (creativeTab == null || creativeTab.internal == internal.getCreativeTab()) {
            res.add(new ItemStack(internal, 1));
        }
        return res;
    }

    /* Overrides */

    public List<String> getTooltip(ItemStack itemStack) {
        return Collections.emptyList();
    }

    public ClickResult onClickBlock(Player player, World world, Vec3i vec3i, Hand from, Facing from1, Vec3d vec3d) {
        return ClickResult.PASS;
    }

    public void onClickAir(Player player, World world, Hand hand) {

    }

    public boolean isValidArmor(ItemStack itemStack, ArmorSlot from, Entity entity) {
        return false;
    }

    public String getCustomName(ItemStack stack) {
        return null;
    }

    /* Name Hacks */

    protected final void applyCustomName(ItemStack stack) {
        String custom = getCustomName(stack);
        if (custom != null) {
            stack.internal.setStackDisplayName(ChatFormatting.RESET + custom);
        }
    }

    public Identifier getRegistryName() {
        return new Identifier(modID, name);
    }

    // Removed 1.7.10 @Optional.Interface(iface = "mezz.jei.api.ingredients.ISlowRenderItem", modid = "jei")
    private class ItemInternal extends Item {
        @Override
        public void getSubItems(Item itemIn, CreativeTabs tab, List items) {
            CreativeTab myTab = tab != CreativeTabs.tabAllSearch && tab != null ? new CreativeTab(tab) : null;
            items.addAll(getItemVariants(myTab).stream().map((ItemStack stack) -> stack.internal).collect(Collectors.toList()));
        }

        @Override
        @SideOnly(Side.CLIENT)
        public final void addInformation(net.minecraft.item.ItemStack stack, EntityPlayer entityPlayer, List tooltip, boolean flagIn) {
            super.addInformation(stack, entityPlayer, tooltip, flagIn);
            applyCustomName(new ItemStack(stack));
            tooltip.addAll(ItemBase.this.getTooltip(new ItemStack(stack)));
        }

        @Override
        public final boolean onItemUse(net.minecraft.item.ItemStack stack, EntityPlayer player, net.minecraft.world.World worldIn, int posX, int posY, int posZ, int facing, float hitX, float hitY, float hitZ) {
            return ItemBase.this.onClickBlock(new Player(player), World.get(worldIn), new Vec3i(posX, posY, posZ), Hand.PRIMARY, Facing.from((byte)facing), new Vec3d(hitX, hitY, hitZ)).internal;
        }

        @Override
        public final net.minecraft.item.ItemStack onItemRightClick(net.minecraft.item.ItemStack stack, net.minecraft.world.World world, EntityPlayer player) {
            onClickAir(new Player(player), World.get(world), Hand.PRIMARY);
            return super.onItemRightClick(stack, world, player);
        }

        @Override
        public final boolean isValidArmor(net.minecraft.item.ItemStack stack, int armorType, net.minecraft.entity.Entity entity) {
            return ItemBase.this.isValidArmor(new ItemStack(stack), ArmorSlot.from(armorType), new Entity(entity));
        }

        @Override
        public final String getUnlocalizedName(net.minecraft.item.ItemStack stack) {
            applyCustomName(new ItemStack(stack));
            return super.getUnlocalizedName(stack);
        }

        @Override
        public final CreativeTabs[] getCreativeTabs() {
            return Arrays.stream(ItemBase.this.creativeTabs).map((CreativeTab tab) -> tab.internal).toArray(CreativeTabs[]::new);
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
            String iconName = ItemRender.getIcon(ItemBase.this);
            if (iconName != null) {
                this.itemIcon = ir.registerIcon(iconName.replace(":items/", ":"));
            } else {
                super.registerIcons(ir);
            }
        }
    }
}
