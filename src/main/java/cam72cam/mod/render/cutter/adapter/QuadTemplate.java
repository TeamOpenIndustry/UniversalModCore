package cam72cam.mod.render.cutter.adapter;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public class QuadTemplate {

    public final TextureAtlasSprite sprite;

    public final Direction direction;

    public final int tintIndex;

    public final boolean shade;

    public final boolean ambientOcclusion;

    public QuadTemplate(
            TextureAtlasSprite sprite,
            Direction direction,
            int tintIndex,
            boolean shade,
            boolean ambientOcclusion) {

        this.sprite = sprite;
        this.direction = direction;
        this.tintIndex = tintIndex;
        this.shade = shade;
        this.ambientOcclusion = ambientOcclusion;
    }
}