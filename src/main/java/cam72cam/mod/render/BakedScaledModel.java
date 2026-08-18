package cam72cam.mod.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import cam72cam.mod.render.cutter.MeshPlaneCutter;
import cam72cam.mod.math.Plane;
import cam72cam.mod.render.cutter.BakedQuadAdapter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.model.QuadTransformer;
import util.Matrix4;

import java.util.*;

/**
 * Internal class to scale an existing Baked Model
 *
 * Do not use directly
 */
class BakedScaledModel implements IBakedModel {
    // I know this is evil and I love it :D

    private final Matrix4 transform;
    private final IBakedModel source;
    private final boolean isCut;
    private final Map<Direction, List<BakedQuad>> quadCache = new HashMap<>();

    private final Random quadRand = new Random(42L);

    public BakedScaledModel(IBakedModel source, Matrix4 transform) {
        this.source = source;
        this.transform = transform;
        this.isCut = false;
    }

    public BakedScaledModel(IBakedModel source, float height) {
        this.source = source;
        transform = new Matrix4().scale(1, height, 1);
        this.isCut = false;
    }

    public BakedScaledModel(IBakedModel source, Matrix4 transform, Plane plane) {
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
            Direction dir = quad.getFace();
            if (dir != null) {
                quadCache.get(dir).add(quad);
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
        if(isCut) {
            return quadCache.getOrDefault(side, Collections.emptyList());
        }

        if (quadCache.get(side) == null) {
            quadCache.put(side, transformQuads(source.getQuads(state, side, rand)));
        }

        return quadCache.get(side);
    }

    private List<BakedQuad> transformQuads(List<BakedQuad> quads) {
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
        return new QuadTransformer(new TransformationMatrix(mat)).processMany(quads);
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
    public ItemOverrideList getOverrides() {
        return source.getOverrides();
    }

}