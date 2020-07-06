package cam72cam.mod.entity;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.util.registry.Registry;

import java.util.UUID;

public class CustomSpawnPacket extends Packet {
    @TagField
    private UUID uuid;
    @TagField
    private Vec3d pos;
    @TagField
    private Vec3d vel;
    @TagField
    private float pitch;
    @TagField
    private float yaw;
    @TagField
    private int typeId;
    @TagField
    private int id;

    @TagField
    private TagCompound entData;

    public CustomSpawnPacket() {
    }

    public CustomSpawnPacket(net.minecraft.entity.Entity entity) {
        //Duplicate EntitySpawnS2CPacket
        id = entity.getEntityId();
        uuid = entity.getUuid();
        pos = new Vec3d(entity.getPosVector());
        vel = new Vec3d(entity.getVelocity());
        pitch = entity.pitch;
        yaw = entity.yaw;
        typeId = Registry.ENTITY_TYPE.getRawId(entity.getType());

        if (entity instanceof IAdditionalSpawnData) {
            TagCompound entData = new TagCompound();
            ((IAdditionalSpawnData)entity).writeSpawnData(entData);
            this.entData = entData;
        }
    }


    @Override
    protected void handle() {
        net.minecraft.entity.Entity entity = Registry.ENTITY_TYPE.get(typeId).create(getWorld().internal);

        entity.setEntityId(id);
        entity.setUuid(uuid);
        entity.updatePosition(pos.x, pos.y, pos.z);
        entity.updateTrackedPosition(pos.x, pos.y, pos.z);
        entity.setVelocity(vel.internal);

        if (entity instanceof IAdditionalSpawnData) {
            ((IAdditionalSpawnData)entity).readSpawnData(entData, yaw, pitch);
        }

        ((ClientWorld)getWorld().internal).addEntity(entity.getEntityId(), entity);
    }

    CustomPayloadS2CPacket toPacket() {
        return new CustomPayloadS2CPacket(getIdent(), getBuffer());
    }
}
