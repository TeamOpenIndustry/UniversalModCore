package cam72cam.mod.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.QuadTransformer;
import net.minecraftforge.common.model.TRSRTransformation;
import util.Matrix4;

import javax.vecmath.Matrix4f;
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
    private final Map<Direction, List<BakedQuad>> quadCache = new HashMap<>();

    public BakedScaledModel(IBakedModel source, Matrix4 transform) {
        this.source = source;
        this.transform = transform;
    }

    public BakedScaledModel(IBakedModel source, float height) {
        this.source = source;
        transform = new Matrix4().scale(1, height, 1);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
        if (quadCache.get(side) == null) {
            Matrix4f mat = new Matrix4f();
            mat.m00 = (float) transform.m00;
            mat.m01 = (float) transform.m01;
            mat.m02 = (float) transform.m02;
            mat.m03 = (float) transform.m03;
            mat.m10 = (float) transform.m10;
            mat.m11 = (float) transform.m11;
            mat.m12 = (float) transform.m12;
            mat.m13 = (float) transform.m13;
            mat.m20 = (float) transform.m20;
            mat.m21 = (float) transform.m21;
            mat.m22 = (float) transform.m22;
            mat.m23 = (float) transform.m23;
            mat.m30 = (float) transform.m30;
            mat.m31 = (float) transform.m31;
            mat.m32 = (float) transform.m32;
            mat.m33 = (float) transform.m33;
            QuadTransformer qt = new QuadTransformer(DefaultVertexFormats.BLOCK, new TRSRTransformation(mat));
            quadCache.put(side, qt.processMany(source.getQuads(state, side, rand)));
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