package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import cam72cam.mod.render.cutter.MeshPlaneCutter;
import cam72cam.mod.math.Plane;
import cam72cam.mod.render.cutter.BakedQuadAdapter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.Direction;
import util.Matrix4;

import javax.vecmath.Vector3f;
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

        List<BakedQuad> all = new ArrayList<>();
        all.addAll(source.getQuads(null, null, quadRand));
        for (Direction dir : Direction.values()) {
            all.addAll(source.getQuads(null, dir, quadRand));
        }

        all = transformQuads(all);
        all = MeshPlaneCutter.cut(all, plane, new BakedQuadAdapter());
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
        // We can't use QuadTransformer here yet.  It's buggy in 1.14.4
        List<BakedQuad> altered = new ArrayList<>();
        for (BakedQuad quad : quads) {
            int[] newData = Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length);

            VertexFormat format = quad.getFormat();

            for (int i = 0; i < 4; ++i) {
                int j = format.getIntegerSize() * i;
                Vec3d vec = new Vec3d(
                        Float.intBitsToFloat(newData[j]),
                        Float.intBitsToFloat(newData[j + 1]),
                        Float.intBitsToFloat(newData[j + 2])
                );
                vec = transform.apply(vec);

                newData[j] = Float.floatToRawIntBits((float) vec.x);
                newData[j + 1] = Float.floatToRawIntBits((float) vec.y);
                newData[j + 2] = Float.floatToRawIntBits((float) vec.z);
            }

            altered.add(new BakedQuad(
                    newData,
                    quad.getTintIndex(),
                    quad.getFace(),
                    quad.getSprite(),
                    quad.shouldApplyDiffuseLighting(),
                    quad.getFormat()
            ));
        }
        return altered;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return source.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return source.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return source.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return source.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return source.getOverrides();
    }

}