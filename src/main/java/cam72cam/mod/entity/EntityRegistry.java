package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EntityRegistry {
    private static final Map<Class<? extends CustomEntity>, String> identifiers = new HashMap<>();
    private static final Map<String, Supplier<CustomEntity>> constructors = new HashMap<>();
    private static String missingResources;

    private EntityRegistry() {

    }

    public static void register(ModCore.Mod mod, Supplier<CustomEntity> ctr, int distance) {
        CustomEntity tmp = ctr.get();
        Class<? extends CustomEntity> type = tmp.getClass();

        CommonEvents.Entity.REGISTER.subscribe(() -> {
            Identifier id = new Identifier(mod.modID(), type.getSimpleName());

            // This has back-compat for older entity names
            // TODO expose updateFreq and vecUpdates
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(ModdedEntity.class, type.getSimpleName(), constructors.size(), ModCore.instance, distance, 20, false);

            identifiers.put(type, id.toString());
            constructors.put(id.toString(), ctr);
        });
    }

    public static Supplier<CustomEntity> getConstructor(String type) {
        return constructors.get(type);
    }

    protected static CustomEntity create(String type, ModdedEntity base) {
        return getConstructor(type).get().setup(base);
    }

    public static CustomEntity create(World world, Class<? extends Entity> cls) {
        //TODO null checks
        ModdedEntity ent = new ModdedEntity(world.internal);
        String id = identifiers.get(cls);
        ent.initSelf(id);
        return ent.getSelf();
    }

    public static void registerEvents() {
        CommonEvents.Entity.REGISTER.subscribe(() -> {
            cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(SeatEntity.class, SeatEntity.class.getSimpleName(), constructors.size()+1, ModCore.instance, 512, 20, false);
        });

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

    /*
    @SideOnly(Side.CLIENT)
    public static void registerClientEvents() {
        ClientEvents.TICK.subscribe(() -> {
            if (missingResources != null && !Minecraft.getMinecraft().isSingleplayer() && Minecraft.getMinecraft().getNetHandler() != null) {
                ModCore.error(missingResources);
                Minecraft.getMinecraft().getNetHandler().getNetworkManager().closeChannel(new ChatComponentText(missingResources));
                Minecraft.getMinecraft().loadWorld(null);
                Minecraft.getMinecraft().displayGuiScreen(new GuiDisconnected(new GuiMultiplayer(new GuiMainMenu()), "disconnect.lost", new ChatComponentText(missingResources)));
                missingResources = null;
            }
        });
    }*/
}
