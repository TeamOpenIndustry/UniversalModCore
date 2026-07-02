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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Implement to create/register a custom item */
public abstract class CustomItem {
    public Item internal;
    private final ResourceLocation identifier;

    public CustomItem(String modID, String name) {
        identifier = ResourceLocation.tryBuild(modID, name);

        Item.Properties props = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, identifier)).stacksTo(getStackSize());
        if (!getCreativeTabs().isEmpty()) {
            for (CreativeTab creativeTab : getCreativeTabs()) {
                creativeTab.inject.add(this);
            }
        }

        CommonEvents.Item.REGISTER.subscribe(helper -> {
            internal = new ItemInternal(props);
            helper.register(identifier, internal);
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
        if (creativeTab == null || getCreativeTabs().contains(creativeTab)) {
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
        return new Identifier(BuiltInRegistries.ITEM.getKey(internal));
    }

    private class ItemInternal extends Item {

        public ItemInternal(Properties p_i48487_1_) {
            super(p_i48487_1_);
        }

        @Override
        public Component getName(net.minecraft.world.item.ItemStack stack) {
            String cn = getCustomName(new ItemStack(stack));
            if (cn != null) {
                return Component.translatable(cn);
            }
            return Component.translatable("item." + identifier + ".name");
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context, List<Component> components, TooltipFlag flagIn) {
            super.appendHoverText(stack, context, components, flagIn);
            if (ModCore.hasResources) {
                components.addAll(CustomItem.this.getTooltip(new ItemStack(stack)).stream().map(Component::literal).toList());
            }
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            return CustomItem.this.onClickBlock(new Player(context.getPlayer()), World.get(context.getLevel()), new Vec3i(context.getClickedPos()), Player.Hand.from(context.getHand()), Facing.from(context.getClickedFace()), new Vec3d(context.getClickLocation().subtract(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ()))).internal;
        }

        @Override
        public InteractionResult use(Level world, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
            Player umcPlayer = new Player(player);
            Player.Hand umcHand = Player.Hand.from(hand);
            onClickAir(umcPlayer, World.get(world), umcHand);
            ItemStack stack = umcPlayer.getHeldItem(umcHand).copy();
            return InteractionResult.CONSUME;
        }
//See constructor
//        @Override
//        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
//            consumer.accept(new IClientItemExtensions() {
//                @Override
//                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
//                    return ItemRender.ISTER();
//                }
//            });
//        }
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
        private ItemStack stack;

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
