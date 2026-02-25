package cam72cam.mod.event.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.Map;

//TODO Hook into resource loading to add dynamically generated json
//I hate CODEC...
public class RegisterDamageTypeEvent extends Event implements IModBusEvent {
    private final Map<ResourceLocation, Resource> map;

    public RegisterDamageTypeEvent(Map<ResourceLocation, Resource> map) {
        this.map = map;
    }
}
