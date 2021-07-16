package cam72cam.mod.render;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import cam72cam.mod.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.QuadTransformer;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.*;

/**
 * Internal class to scale an existing Baked Model
 *
 * Do not use directly
 */
class BakedScaledModel implements IBakedModel {
    // I know this is evil and I love it :D

    private final Vec3d scale;
    private final Vec3d transform;
    private final IBakedModel source;
    private final Map<Direction, List<BakedQuad>> quadCache = new HashMap<>();

    public BakedScaledModel(IBakedModel source, Vec3d scale, Vec3d transform) {
        this.source = source;
        this.scale = scale;
        this.transform = transform;
    }

    public BakedScaledModel(IBakedModel source, float height) {
        this.source = source;
        this.scale = new Vec3d(1, height, 1);
        transform = new Vec3d(0, 0, 0);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
        if (quadCache.get(side) == null) {
            Matrix4f mat = new Matrix4f();
            mat.m00 = (float)scale.x;
            mat.m11 = (float)scale.y;
            mat.m22 = (float)scale.z;
            mat.m33 = 1.0F;
            mat.setTranslation(new Vector3f((float)transform.x, (float)transform.y, (float)transform.z));
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