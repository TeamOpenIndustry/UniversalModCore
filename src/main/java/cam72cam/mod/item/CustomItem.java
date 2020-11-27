package cam72cam.mod.item;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.TextUtil;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Implement to create/register a custom item */
public abstract class CustomItem {
    public final Item internal;
    private final ResourceLocation identifier;

    public CustomItem(String modID, String name) {
        identifier = new ResourceLocation(modID, name);

        Item.Properties props = new Item.Properties().maxStackSize(getStackSize()).group(getCreativeTabs().get(0).internal);
        Item.Properties propsWithRender = DistExecutor.unsafeRunForDist(() -> () -> props.setISTER(ItemRender::ISTER), () -> () -> props);

        internal = new ItemInternal(propsWithRender);
        internal.setRegistryName(identifier);

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
    public final Identifier getRegistryName() {
        return new Identifier(internal.getRegistryName());
    }

    private class ItemInternal extends Item {

        public ItemInternal(Properties p_i48487_1_) {
            super(p_i48487_1_);
        }

        @Override
        public ITextComponent getDisplayName(net.minecraft.item.ItemStack stack) {
            String custom = getCustomName(new ItemStack(stack));
            if (custom != null) {
                return new StringTextComponent(custom);
            }
            //return new StringTextComponent(TextUtil.translate(getTranslationKey(stack)));
            return super.getDisplayName(stack);
        }

        @Override
        public void fillItemGroup(ItemGroup tab, NonNullList<net.minecraft.item.ItemStack> items) {
            CreativeTab myTab = tab != ItemGroup.SEARCH ? new CreativeTab(tab) : null;
            if (ModCore.hasResources) {
                items.addAll(getItemVariants(myTab).stream().map((ItemStack stack) -> stack.internal).collect(Collectors.toList()));
            }
        }

        @Override
        public String getTranslationKey(net.minecraft.item.ItemStack stack) {
            String cn = getCustomName(new ItemStack(stack));
            if (cn != null) {
                return cn;
            }
            return "item." + identifier + ".name";
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public final void addInformation(net.minecraft.item.ItemStack stack, @Nullable net.minecraft.world.World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
            super.addInformation(stack, worldIn, tooltip, flagIn);
            if (ModCore.hasResources) {
                tooltip.addAll(CustomItem.this.getTooltip(new ItemStack(stack)).stream().map(StringTextComponent::new).collect(Collectors.toList()));
            }
        }

        @Override
        public ActionResultType onItemUse(ItemUseContext context) {
            return CustomItem.this.onClickBlock(new Player(context.getPlayer()), World.get(context.getWorld()), new Vec3i(context.getPos()), Player.Hand.from(context.getHand()), Facing.from(context.getFace()), new Vec3d(context.getHitVec().subtract(context.getPos().getX(), context.getPos().getY(), context.getPos().getZ()))).internal;
        }

        @Override
        public ActionResult<net.minecraft.item.ItemStack> onItemRightClick(net.minecraft.world.World world, PlayerEntity player, net.minecraft.util.Hand hand) {
            onClickAir(new Player(player), World.get(world), Player.Hand.from(hand));
            return super.onItemRightClick(world, player, hand);
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
