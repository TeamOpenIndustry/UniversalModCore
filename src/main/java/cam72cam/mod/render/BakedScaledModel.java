package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
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
    private final Map<EnumFacing, List<BakedQuad>> quadCache = new HashMap<>();

    public BakedScaledModel(IBakedModel source, Matrix4 transform) {
        this.source = source;
        this.transform = transform;
    }

    public BakedScaledModel(IBakedModel source, float height) {
        this.source = source;
        transform = new Matrix4().scale(1, height, 1);
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        if (quadCache.get(side) == null) {
            List<BakedQuad> quads = source.getQuads(state, side, rand);
            quadCache.put(side, new ArrayList<>());
            for (BakedQuad quad : quads) {
                int[] newData = Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length);

                VertexFormat format = quad.getFormat();

                for (int i = 0; i < 4; ++i) {
                    int j = format.getIntegerSize() * i;
                    Vec3d vec = new Vec3d(
                            Float.intBitsToFloat(newData[j + 0]),
                            Float.intBitsToFloat(newData[j + 1]),
                            Float.intBitsToFloat(newData[j + 2])
                    );
                    transform.apply(vec);

                    newData[j + 0] = Float.floatToRawIntBits((float) vec.x);
                    newData[j + 1] = Float.floatToRawIntBits((float) vec.y);
                    newData[j + 2] = Float.floatToRawIntBits((float) vec.z);
                }

                quadCache.get(side).add(new BakedQuad(newData, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat()));
            }
        }

        return quadCache.get(side);
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