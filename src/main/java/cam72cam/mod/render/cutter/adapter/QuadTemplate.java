package cam72cam.mod.render.cutter.adapter;

import cam72cam.mod.util.Facing;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class QuadTemplate {

    public final TextureAtlasSprite sprite;

    public final Facing facing;

    public final int tintIndex;

    public final boolean shade;

    public final boolean ambientOcclusion;

    public final VertexFormat format; // Should never be used, only for 1.12 compat

    public QuadTemplate(
            TextureAtlasSprite sprite,
            Facing facing,
            int tintIndex,
            boolean shade,
            boolean ambientOcclusion,
            VertexFormat format) {

        this.sprite = sprite;
        this.facing = facing;
        this.tintIndex = tintIndex;
        this.shade = shade;
        this.ambientOcclusion = ambientOcclusion;
        this.format = format;
    }
}