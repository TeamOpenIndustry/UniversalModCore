package cam72cam.mod.render.cutter.adapter;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import cam72cam.mod.util.Facing;
import net.minecraft.util.EnumFacing;

import java.util.List;

public class QuadTemplate {

    public final TextureAtlasSprite sprite;

    public final Facing facing;

    public final Facing sourceFace;

    public final int tintIndex;

    public final boolean shade;

    public final boolean ambientOcclusion;

    public final VertexFormat format;

    public final BakedQuad source;

    public final List<BakedQuad> candidates;


    public QuadTemplate(
            TextureAtlasSprite sprite,
            Facing facing,
            int tintIndex,
            boolean shade,
            boolean ambientOcclusion,
            VertexFormat format,
            BakedQuad source,
            List<BakedQuad> candidates) {

        this.sprite = sprite;
        this.facing = facing;
        this.tintIndex = tintIndex;
        this.shade = shade;
        this.ambientOcclusion = ambientOcclusion;
        this.format = format;

        this.source = source;
        this.candidates = candidates;

        this.sourceFace = Facing.from(source.getFace());
    }
}