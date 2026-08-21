package cam72cam.mod.render;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import cam72cam.mod.render.cutter.MeshPlaneCutter;
import cam72cam.mod.math.Plane;
import cam72cam.mod.render.cutter.BakedQuadAdapter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.QuadTransformers;
import org.joml.Matrix4f;
import util.Matrix4;

import java.util.*;

/**
 * Internal class to scale an existing Baked Model
 * <p>
 * Do not use directly
 */
class BakedScaledModel implements BakedModel {
    // I know this is evil and I love it :D

    private final Matrix4 transform;
    private final BakedModel source;
    private final boolean isCut;
    private final Map<Direction, List<BakedQuad>> quadCache = new HashMap<>();

    private final RandomSource quadRand = RandomSource.create();

    public BakedScaledModel(BakedModel source, Matrix4 transform) {
        this.source = source;
        this.transform = transform;
        this.isCut = false;
    }

    public BakedScaledModel(BakedModel source, float height) {
        this.source = source;
        transform = new Matrix4().scale(1, height, 1);
        this.isCut = false;
    }

    public BakedScaledModel(BakedModel source, Matrix4 transform, Plane plane) {
        this.source = source;
        this.transform = transform;
        this.isCut = true;

        quadCache.put(null, new ArrayList<>());
        for (Direction dir : Direction.values()) {
            quadCache.put(dir, new ArrayList<>());
        }

        List<BakedQuad> all = new ArrayList<>(source.getQuads(null, null, quadRand));
        for (Direction dir : Direction.values()) {
            all.addAll(source.getQuads(null, dir, quadRand));
        }

        all = MeshPlaneCutter.cut(transformQuads(all), plane, new BakedQuadAdapter());
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
            return quadCache.getOrDefault(side, Collections.emptyList());
        }

        if (quadCache.get(side) == null) {
            quadCache.put(side, transformQuads(source.getQuads(state, side, rand)));
        }

        return quadCache.get(side);
    }

    private List<BakedQuad> transformQuads(List<BakedQuad> quads) {
        Matrix4f mat = transform.convertToMoj();
        return QuadTransformers.applying(new Transformation(mat)).process(quads);
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