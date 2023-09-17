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
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IItemRenderProperties;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Implement to create/register a custom item */
public abstract class CustomItem {
    public Item internal;
    private final ResourceLocation identifier;

    public CustomItem(String modID, String name) {
        identifier = new ResourceLocation(modID, name);

        CommonEvents.Item.REGISTER.subscribe(() -> {
            Item.Properties props = new Item.Properties().stacksTo(getStackSize());
            if (!getCreativeTabs().isEmpty()) {
                props = props.tab(getCreativeTabs().get(0).internal);
            }

            internal = new ItemInternal(props);
            internal.setRegistryName(identifier);

            ForgeRegistries.ITEMS.register(internal);
        });
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
        if (creativeTab == null || creativeTab.internal == internal.getItemCategory()) {
            res.add(new ItemStack(new net.minecraft.world.item.ItemStack(internal, 1)));
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
        public Component getName(net.minecraft.world.item.ItemStack stack) {
            String custom = getCustomName(new ItemStack(stack));
            if (custom != null) {
                return new TextComponent(custom);
            }
            //return new StringTextComponent(TextUtil.translate(getTranslationKey(stack)));
            return super.getName(stack);
        }

        @Override
        public void fillItemCategory(CreativeModeTab tab, NonNullList<net.minecraft.world.item.ItemStack> items) {
            CreativeTab myTab = tab != CreativeModeTab.TAB_SEARCH ? new CreativeTab(tab) : null;
            if (ModCore.hasResources) {
                items.addAll(getItemVariants(myTab).stream().map((ItemStack stack) -> stack.internal).collect(Collectors.toList()));
            }
        }

        @Override
        public String getDescriptionId(net.minecraft.world.item.ItemStack stack) {
            String cn = getCustomName(new ItemStack(stack));
            if (cn != null) {
                return cn;
            }
            return "item." + identifier + ".name";
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public final void appendHoverText(net.minecraft.world.item.ItemStack stack, @Nullable net.minecraft.world.level.Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
            super.appendHoverText(stack, worldIn, tooltip, flagIn);
            if (ModCore.hasResources) {
                tooltip.addAll(CustomItem.this.getTooltip(new ItemStack(stack)).stream().map(TextComponent::new).collect(Collectors.toList()));
            }
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            return CustomItem.this.onClickBlock(new Player(context.getPlayer()), World.get(context.getLevel()), new Vec3i(context.getClickedPos()), Player.Hand.from(context.getHand()), Facing.from(context.getClickedFace()), new Vec3d(context.getClickLocation().subtract(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ()))).internal;
        }

        @Override
        public InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level world, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
            onClickAir(new Player(player), World.get(world), Player.Hand.from(hand));
            return super.use(world, player, hand);
        }

        @Override
        public void initializeClient(Consumer<IItemRenderProperties> consumer) {
            consumer.accept(new IItemRenderProperties() {
                @Override
                public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
                    return ItemRender.ISTER();
                }
            });
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
