package cam72cam.mod.render.cutter.adapter;

import cam72cam.mod.util.Facing;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class QuadTemplate {

    public final TextureAtlasSprite sprite;

    public final Facing facing;

    public final int tintIndex;

    public final boolean shade;

    public final boolean ambientOcclusion;

    public QuadTemplate(
            TextureAtlasSprite sprite,
            Facing facing,
            int tintIndex,
            boolean shade,
            boolean ambientOcclusion) {

        this.sprite = sprite;
        this.facing = facing;
        this.tintIndex = tintIndex;
        this.shade = shade;
        this.ambientOcclusion = ambientOcclusion;
    }
}