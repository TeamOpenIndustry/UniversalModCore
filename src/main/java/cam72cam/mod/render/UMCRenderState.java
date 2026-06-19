package cam72cam.mod.render;

import cam72cam.mod.entity.ModdedEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UMCRenderState extends EntityRenderState {
    public ModdedEntity entity;
    public float rotationYaw;
}
