package cam72cam.mod.render;

import cam72cam.mod.render.cutter.MeshPlaneCutter;
import cam72cam.mod.render.cutter.Plane;
import cam72cam.mod.render.cutter.adapter.BakedQuadAdapter;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
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

import java.util.ArrayList;
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

    private final boolean isCut;
    private final Matrix4 transform;
    private final BakedModel source;
    private final Map<Direction, List<BakedQuad>> quadCache = new HashMap<>();

    public BakedScaledModel(BakedModel source, Matrix4 transform) {
        this.source = source;
        this.transform = transform;
        this.isCut = false;
    }

    public BakedScaledModel(BakedModel source, float height) {
        this.source = source;
        transform = new Matrix4().scale(1, height, 1);
        isCut = false;
    }

    public BakedScaledModel(BakedModel source, Matrix4 transform, Plane plane) {

        this.source = source;
        this.transform = transform;
        this.isCut = true;

        quadCache.put(null, new ArrayList<>());

        for (Direction dir : Direction.values()) {
            quadCache.put(dir, new ArrayList<>());
        }

        RandomSource rand = RandomSource.create();

        List<BakedQuad> all = new ArrayList<>();

        all.addAll(source.getQuads(null, null, rand));

        for (Direction dir : Direction.values()) {
            all.addAll(source.getQuads(null, dir, rand));
        }

        all = MeshPlaneCutter.cut(
                all,
                plane,
                new BakedQuadAdapter()
        );

        Matrix4f mat = transform.convertToMoj();

        IQuadTransformer qt =
                QuadTransformers.applying(
                        new Transformation(mat));

        all = qt.process(all);

        for (BakedQuad quad : all) {

            quadCache.get(null).add(quad);

            Direction dir = quad.getDirection();

            if (dir != null) {
                quadCache.get(dir).add(quad);
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
        if(isCut) {
            return quadCache.getOrDefault(
                    side,
                    List.of()
            );
        }

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