package cam72cam.mod.render;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import org.joml.Matrix4f;
import util.Matrix4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal class to scale an existing Baked Model
 * <p>
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
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
        if (quadCache.get(side) == null) {
            Matrix4f mat = transform.convertToMoj();
            IQuadTransformer qt = QuadTransformers.applying(new Transformation(mat));
            quadCache.put(side, qt.process(source.getQuads(state, side, rand)));
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
    public TextureAtlasSprite getParticleIcon() {
        return source.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return source.getTransforms();
    }
}