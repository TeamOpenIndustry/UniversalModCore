package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EntityRegistry {
    private static final Map<Class<? extends CustomEntity>, EntityType<? extends ModdedEntity>> registered = new HashMap<>();
    private static final Map<String, Supplier<CustomEntity>> constructors = new HashMap<>();

    private static String missingResources;

    private EntityRegistry() {

    }

    public static void register(ModCore.Mod mod, Supplier<CustomEntity> ctr, int distance) {
        CustomEntity tmp = ctr.get();
        Class<? extends CustomEntity> type = tmp.getClass();
        Identifier id = new Identifier(mod.modID(), type.getSimpleName());

        // TODO expose updateFreq and vecUpdates

        CommonEvents.Entity.REGISTER.subscribe(() -> {
            EntityType.EntityFactory<ModdedEntity> factory = (et, world) -> new ModdedEntity(et, world, ctr);
            EntityType.Builder<ModdedEntity> builder = EntityType.Builder.of(factory, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(false)
                    .setTrackingRange(distance)
                    .setUpdateInterval(20)
                    .setCustomClientFactory((se, world) -> new ModdedEntity(registered.get(type), world, ctr));
            if (ctr.get().isImmuneToFire()) {
                builder = builder.fireImmune();
            }
            EntityType<? extends ModdedEntity> et = builder.build(id.toString());
            ForgeRegistries.ENTITY_TYPES.register(id.internal, et);
            registered.put(type, et);
        });

        constructors.put(id.toString(), ctr);
    }

    public static EntityType<? extends ModdedEntity> type(Class<? extends Entity> cls) {
        return registered.get(cls);
    }

    public static CustomEntity create(World world, Class<? extends Entity> cls) {
        //TODO null checks
        ModdedEntity ent = registered.get(cls).create(world.internal);
        return ent.getSelf();
    }

    public static void registerEvents() {
        CommonEvents.Entity.REGISTER.subscribe(() -> ForgeRegistries.ENTITY_TYPES.register(SeatEntity.ID, SeatEntity.TYPE));
        CommonEvents.Entity.JOIN.subscribe((world, entity) -> {
            if (entity instanceof ModdedEntity) {
                if (World.get(world) != null) {
                    Pair<String, TagCompound> msg = ((ModdedEntity) entity).refusedToJoin;
                    if (msg != null) {
                        missingResources = msg.getKey();
                        return false;
                    }
                }
            }
            return true;
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerClientEvents() {
        ClientEvents.TICK.subscribe(() -> {
            if (missingResources != null && !Minecraft.getInstance().hasSingleplayerServer() && Minecraft.getInstance().getConnection() != null) {
                ModCore.error(missingResources);
                Minecraft.getInstance().getConnection().getConnection().disconnect(PlayerMessage.direct(missingResources).internal);
                Minecraft.getInstance().clearLevel();
                Minecraft.getInstance().setScreen(new DisconnectedScreen(new JoinMultiplayerScreen(new TitleScreen()), Component.translatable("disconnect.lost"), PlayerMessage.direct(missingResources).internal));
                missingResources = null;
            }
        });
        CommonEvents.World.UNLOAD.subscribe(w -> {
            if (w.isClientSide) {
                // Cleanup client side since mc does not call setDead client side...
                // See ClientEvents registration for related crap
                for (net.minecraft.world.entity.Entity entity : ((ClientLevel) w).entitiesForRendering()) {
                    if (entity instanceof ModdedEntity) {
                        entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                    }
                }
            }
        });
    }

    static CustomEntity create(String custom_mob_type, ModdedEntity base) {
        return constructors.get(custom_mob_type).get().setup(base);
    }
}
