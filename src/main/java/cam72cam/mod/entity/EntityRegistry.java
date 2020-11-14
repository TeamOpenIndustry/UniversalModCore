package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EntityRegistry {
    private static Map<Class<? extends CustomEntity>, EntityType<ModdedEntity>> registered = new HashMap<>();

    private static String missingResources;

    private EntityRegistry() {

    }

    public static void register(ModCore.Mod mod, Supplier<CustomEntity> ctr, int distance) {
        CustomEntity tmp = ctr.get();
        Class<? extends CustomEntity> type = tmp.getClass();
        Identifier id = new Identifier(mod.modID(), type.getSimpleName().toLowerCase());

        EntityType.EntityFactory<ModdedEntity> factory = (et, world) -> new ModdedEntity(et, world, ctr);
        FabricEntityTypeBuilder<ModdedEntity> builder = FabricEntityTypeBuilder.create(EntityCategory.MISC, factory)
                .trackable(distance, 20, false);
        if (tmp.isImmuneToFire()) {
            builder = builder.setImmuneToFire();
        }
        EntityType<ModdedEntity> oet = Registry.register(Registry.ENTITY_TYPE, id.internal, builder.build());
        registered.put(type, oet);
    }

    public static CustomEntity create(World world, Class<? extends Entity> cls) {
        return registered.get(cls).create(world.internal).getSelf();
    }

    public static void registerEvents() {
        SeatEntity.TYPE = Registry.register(Registry.ENTITY_TYPE, SeatEntity.ID, FabricEntityTypeBuilder.create(EntityCategory.MISC, SeatEntity::new).trackable(512, 20, false).build());

        CommonEvents.Entity.JOIN.subscribe((world, entity) -> {
            if (entity instanceof ModdedEntity) {
                if (World.get(world) != null) {
                    String msg = ((ModdedEntity) entity).getSelf().tryJoinWorld();
                    if (msg != null) {
                        missingResources = msg;
                        return false;
                    }
                }
            }
            return true;
        });
    }

    @Environment(EnvType.CLIENT)
    public static void registerClientEvents() {
        ClientEvents.TICK.subscribe(() -> {
            if (missingResources != null && !MinecraftClient.getInstance().isInSingleplayer() && MinecraftClient.getInstance().getNetworkHandler() != null) {
                ModCore.error(missingResources);
                MinecraftClient.getInstance().getNetworkHandler().getConnection().disconnect(PlayerMessage.direct(missingResources).internal);
                MinecraftClient.getInstance().joinWorld(null);
                MinecraftClient.getInstance().openScreen(new DisconnectedScreen(new MultiplayerScreen(new TitleScreen()), "disconnect.lost", PlayerMessage.direct(missingResources).internal));
                missingResources = null;
            }
        });
    }
}
