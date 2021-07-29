package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
import com.mojang.math.Matrix4f;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.QuadTransformer;

import java.util.*;

/**
 * Internal class to scale an existing Baked Model
 *
 * Do not use directly
 */
class BakedScaledModel implements BakedModel {
    // I know this is evil and I love it :D

    private final Vec3d scale;
    private final Vec3d transform;
    private final BakedModel source;
    private final Map<Direction, List<BakedQuad>> quadCache = new HashMap<>();

    public BakedScaledModel(BakedModel source, Vec3d scale, Vec3d transform) {
        this.source = source;
        this.scale = scale;
        this.transform = transform;
    }

    public BakedScaledModel(BakedModel source, float height) {
        this.source = source;
        this.scale = new Vec3d(1, height, 1);
        transform = new Vec3d(0, 0, 0);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
        if (quadCache.get(side) == null) {
            Matrix4f mat = Matrix4f.createScaleMatrix((float)scale.x, (float)scale.y, (float)scale.z);
            mat.translate(new Vector3f((float)transform.x, (float)transform.y, (float)transform.z));
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