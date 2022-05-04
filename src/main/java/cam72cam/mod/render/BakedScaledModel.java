package cam72cam.mod.render;

import com.mojang.math.Matrix4f;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.QuadTransformer;
import util.Matrix4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Internal class to scale an existing Baked Model
 *
 * Do not use directly
 */
class BakedScaledModel implements BakedModel {
    // I know this is evil and I love it :D

    private final Matrix4 transform;
    private final BakedModel source;
    private final Map<Direction, List<BakedQuad>> quadCache = new HashMap<>();

    public BakedScaledModel(BakedModel source, Matrix4 transform) {
        this.source = source;
        this.transform = transform;
    }

    public BakedScaledModel(BakedModel source, float height) {
        this.source = source;
        transform = new Matrix4().scale(1, height, 1);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
        if (quadCache.get(side) == null) {
            Matrix4f mat = new Matrix4f(new float[] {
                    (float) transform.m00,
                    (float) transform.m01,
                    (float) transform.m02,
                    (float) transform.m03,
                    (float) transform.m10,
                    (float) transform.m11,
                    (float) transform.m12,
                    (float) transform.m13,
                    (float) transform.m20,
                    (float) transform.m21,
                    (float) transform.m22,
                    (float) transform.m23,
                    (float) transform.m30,
                    (float) transform.m31,
                    (float) transform.m32,
                    (float) transform.m33
            });
            QuadTransformer qt = new QuadTransformer(new Transformation(mat));
            quadCache.put(side, qt.processMany(source.getQuads(state, side, rand)));
        }

        return quadCache.get(side);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return source.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return source.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return source.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return source.getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return source.getOverrides();
    }

    @Override
    public ItemTransforms getTransforms() {
        return source.getTransforms();
    }
}