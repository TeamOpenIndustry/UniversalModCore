package cam72cam.mod.gui;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.gui.container.ClientContainerBuilder;
import cam72cam.mod.gui.container.IContainer;
import cam72cam.mod.gui.container.ServerContainerBuilder;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.ScreenBuilder;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.CRC32;

/** GUI Registry for all types of GUIs */
public class GuiRegistry {
    private static final Map<Integer, Function<CreateEvent, ServerContainerBuilder>> registry = new HashMap<>();

    private static final ContainerType<ServerContainerBuilder> TYPE = IForgeContainerType.create(
            (id, inv, data) -> registry.get(data.readInt()).apply(new CreateEvent(id, inv, data.readInt(), data.readInt(), data.readInt()))
    );
    static {
        TYPE.setRegistryName(new ResourceLocation(ModCore.MODID, "alltheguis"));
    }

    public static void registerEvents() {
        CommonEvents.CONTAINER_REGISTRY.subscribe(reg -> reg.register(TYPE));
    }


    /** Internal event registration, do not use */
    @OnlyIn(Dist.CLIENT)
    public static void registerClientEvents() {
        ClientEvents.REGISTER_ENTITY.subscribe(() -> ScreenManager.registerFactory(TYPE, ClientContainerBuilder::new));
    }

    public GuiRegistry() {
    }

    /** Handle to a GUI that can be opened both client and server side */
    @FunctionalInterface
    public interface GUI {
        void open(Player player);
    }

    /** Handle to a Entity GUI that can be opened both client and server side */
    @FunctionalInterface
    public interface EntityGUI<T extends Entity> {
        void open(Player player, T entity);
    }

    /** Handle to a Block GUI that can be opened both client and server side */
    @FunctionalInterface
    public interface BlockGUI {
        void open(Player player, Vec3i pos);
    }

    /** Here's hoping we don't get collisions... */
    private static int intFromName(String s) {
        CRC32 hasher = new CRC32();
        hasher.update(s.length());
        hasher.update(s.getBytes());
        return (int) hasher.getValue();
    }

    @OnlyIn(Dist.CLIENT)
    private static void openScreen(IScreen screen, Supplier<Boolean> valid) {
        Minecraft.getInstance().displayGuiScreen(new ScreenBuilder(screen, valid));
    }

    /** Register a standalone GUI */
    public static GUI register(Identifier name, Supplier<IScreen> ctr) {
        int id = intFromName(name.toString());
        // TODO server packet with ID
        return (player) -> DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> openScreen(ctr.get(), () -> true));
    }

    /** Register a Block based GUI */
    public static <T extends BlockEntity> BlockGUI registerBlock(Class<T> cls, Function<T, IScreen> ctr) {
        int id = intFromName(cls.toString());
        // TODO server packet with ID
        return (player, pos) -> {
            DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
                T entity = player.getWorld().getBlockEntity(pos, cls);
                if (entity == null) {
                    return;
                }
                IScreen screen = ctr.apply(entity);
                if (screen == null) {
                    return;
                }

                openScreen(screen, () -> !entity.internal.isRemoved());
            });
        };
    }

    /** Register a Block based Container */
    public static <T extends BlockEntity> BlockGUI registerBlockContainer(Class<T> cls, Function<T, IContainer> ctr) {
        int id = intFromName(("container" + cls.toString()));

        registry.put(id, event -> {
            T entity = World.get(event.inv.player.world).getBlockEntity(new Vec3i(event.entityIDorX, event.y, event.z), cls);
            if (entity == null) {
                return null;
            }
            return new ServerContainerBuilder(event.id, TYPE, event.inv, ctr.apply(entity), () -> !entity.internal.isRemoved());
        });
        return (player, pos) -> {
            if (!(player.internal instanceof ServerPlayerEntity)) {
                System.out.println("PROBS SHOULD SEND PKT");
                return;
            }
            NetworkHooks.openGui((ServerPlayerEntity) player.internal, new INamedContainerProvider() {
                @Override
                public ITextComponent getDisplayName() {
                    return new StringTextComponent("");
                }

                @Nullable
                @Override
                public Container createMenu(int p_createMenu_1_, PlayerInventory p_createMenu_2_, PlayerEntity p_createMenu_3_) {
                    return registry.get(id).apply(new CreateEvent(p_createMenu_1_, p_createMenu_2_, pos.x, pos.y, pos.z));
                }
            }, (buff) -> {
                buff.writeInt(id);
                buff.writeInt(pos.x);
                buff.writeInt(pos.y);
                buff.writeInt(pos.z);
            });
        };
    }

    /** Register a Entity based GUI */
    public static <T extends Entity> EntityGUI<T> registerEntity(Class<T> cls, Function<T, IScreen> ctr) {
        int id = intFromName(cls.toString());

        return (player, entity) -> {
            DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
                if (entity == null) {
                    return;
                }
                IScreen screen = ctr.apply(entity);
                if (screen == null) {
                    return;
                }

                openScreen(screen, entity::isLiving);
            });
        };
    }

    /** Register a Entity based Container */
    public static <T extends Entity> EntityGUI<T> registerEntityContainer(Class<T> cls, Function<T, IContainer> ctr) {
        int id = intFromName(("container" + cls.toString()));
        registry.put(id, event -> {
            T entity = World.get(event.inv.player.world).getEntity(event.entityIDorX, cls);
            if (entity == null) {
                return null;
            }
            return new ServerContainerBuilder(event.id, TYPE, event.inv, ctr.apply(entity), () -> entity == World.get(event.inv.player.world).getEntity(event.entityIDorX, cls));
        });

        return (player, ent) -> {
            if (!(player.internal instanceof ServerPlayerEntity)) {
                System.out.println("PROBS SHOULD SEND PKT");
                return;
            }
            NetworkHooks.openGui((ServerPlayerEntity) player.internal, new INamedContainerProvider() {
                @Override
                public ITextComponent getDisplayName() {
                    return new StringTextComponent("");
                }

                @Nullable
                @Override
                public Container createMenu(int p_createMenu_1_, PlayerInventory p_createMenu_2_, PlayerEntity p_createMenu_3_) {
                    return registry.get(id).apply(new CreateEvent(p_createMenu_1_, p_createMenu_2_, ent.getId(), 0, 0));
                }
            }, (buff) -> {
                buff.writeInt(id);
                buff.writeInt(ent.getId());
                buff.writeInt(0);
                buff.writeInt(0);
            });
        };
    }

    /** Used to represent a client or server create event (for passing params) */
    private static class CreateEvent {
        final PlayerInventory inv;
        final int entityIDorX;
        final int y;
        final int z;
        final int id;

        private CreateEvent(int id, PlayerInventory inv, int entityIDorX, int y, int z) {
            this.id = id;
            this.inv = inv;
            this.entityIDorX = entityIDorX;
            this.y = y;
            this.z = z;
        }
    }
}
