package cam72cam.mod.entity;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.util.TagCompound;
import io.netty.buffer.Unpooled;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.registry.Registry;

public class CustomSpawnPacket extends Packet {

    public CustomSpawnPacket() {
    }

    public CustomSpawnPacket(net.minecraft.entity.Entity entity) {
        //Duplicate EntitySpawnS2CPacket
        data.setInteger("id", entity.getEntityId());
        data.setUUID("uuid", entity.getUuid());
        data.setVec3d("pos", new Vec3d(entity.getPosVector()));
        data.setVec3d("vel", new Vec3d(entity.getVelocity()));
        data.setFloat("pitch", entity.pitch);
        data.setFloat("yaw", entity.yaw);
        data.setInteger("typeId", Registry.ENTITY_TYPE.getRawId(entity.getType()));

        if (entity instanceof IAdditionalSpawnData) {
            TagCompound entData = new TagCompound();
            ((IAdditionalSpawnData)entity).writeSpawnData(entData);
            this.data.set("entData", entData);
        }
    }


    @Override
    protected void handle() {
        net.minecraft.entity.Entity entity = Registry.ENTITY_TYPE.get(data.getInteger("typeId")).create(getWorld().internal);

        entity.setEntityId(data.getInteger("id"));
        entity.setUuid(data.getUUID("uuid"));
        Vec3d pos = data.getVec3d("pos");
        entity.setPosition(pos.x, pos.y, pos.z);
        entity.updateTrackedPosition(pos.x, pos.y, pos.z);
        entity.setVelocity(data.getVec3d("vel").internal);

        if (entity instanceof IAdditionalSpawnData) {
            ((IAdditionalSpawnData)entity).readSpawnData(data.get("entData"), data.getFloat("yaw"), data.getFloat("pitch"));
        }

        getWorld().internal.spawnEntity(entity);
    }

    CustomPayloadS2CPacket toPacket() {
        PacketByteBuf buff = new PacketByteBuf(Unpooled.buffer());
        buff.writeCompoundTag(data.internal);
        return new CustomPayloadS2CPacket(getIdent(), buff);
    }
}
