package cam72cam.mod.render;

import cam72cam.mod.render.cutter.MeshPlaneCutter;
import cam72cam.mod.math.Plane;
import cam72cam.mod.render.cutter.BakedQuadAdapter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
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
    private final Map<EnumFacing, List<BakedQuad>> quadCache = new HashMap<>();

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
        for (EnumFacing dir : EnumFacing.values()) {
            quadCache.put(dir, new ArrayList<>());
        }

        long rand = 0;// In 1.12.2 this means seed
        List<BakedQuad> all = new ArrayList<>();
        all.addAll(source.getQuads(null, null, rand));
        for (EnumFacing dir : EnumFacing.values()) {
            all.addAll(source.getQuads(null, dir, rand));
        }

        all = transformQuads(all);
        all = MeshPlaneCutter.cut(all, plane, new BakedQuadAdapter());
        for (BakedQuad quad : all) {
            quadCache.get(null).add(quad);
            EnumFacing dir = quad.getFace();
            if (dir != null) {
                quadCache.get(dir).add(quad);
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        if(isCut) {
            return quadCache.getOrDefault(side, Collections.emptyList());
        }

        if (quadCache.get(side) == null) {
            quadCache.put(side, transformQuads(source.getQuads(state, side, rand)));
        }

        return quadCache.get(side);
    }

    private List<BakedQuad> transformQuads(List<BakedQuad> quads) {
        List<BakedQuad> result = new ArrayList<>(quads.size());

        for (BakedQuad quad : quads) {
            int[] newData = Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length);
            VertexFormat format = quad.getFormat();

            for (int i = 0; i < 4; i++) {
                int j = format.getIntegerSize() * i;
                Vector3f vec = new Vector3f(
                        Float.intBitsToFloat(newData[j]),
                        Float.intBitsToFloat(newData[j + 1]),
                        Float.intBitsToFloat(newData[j + 2])
                );
                transform.apply(vec);
                newData[j]     = Float.floatToRawIntBits(vec.x);
                newData[j + 1] = Float.floatToRawIntBits(vec.y);
                newData[j + 2] = Float.floatToRawIntBits(vec.z);
            }

            result.add(new BakedQuad(
                    newData,
                    quad.getTintIndex(),
                    quad.getFace(),
                    quad.getSprite(),
                    quad.shouldApplyDiffuseLighting(),
                    quad.getFormat()
            ));
        }

        return result;
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