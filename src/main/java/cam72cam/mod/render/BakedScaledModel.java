package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import java.util.*;

public class BakedScaledModel implements BakedModel {
    // I know this is evil and I love it :D

    private final Vec3d scale;
    private final Vec3d transform;
    private BakedModel source;
    private Map<Direction, List<BakedQuad>> quadCache = new HashMap<>();

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
            List<BakedQuad> quads = source.getQuads(state, side, rand);
            quadCache.put(side, new ArrayList<>());
            for (BakedQuad quad : quads) {
                int[] newData = Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length);

                for (int i = 0; i < 4; ++i) {
                    int j = i * 7;
                    newData[j + 0] = Float.floatToRawIntBits(Float.intBitsToFloat(newData[j + 0]) * (float) scale.x + (float) transform.x);
                    newData[j + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(newData[j + 1]) * (float) scale.y + (float) transform.y);
                    newData[j + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(newData[j + 2]) * (float) scale.z + (float) transform.z);
                }

                quadCache.get(side).add(new BakedQuad(newData, quad.getColorIndex(), quad.getFace(), quad.getSprite()));
            }
        }

        return quadCache.get(side);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return source.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepthInGui() {
        return source.hasDepthInGui();
    }

    @Override
    public boolean isBuiltin() {
        return source.isBuiltin();
    }

    @Override
    public Sprite getSprite() {
        return source.getSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return source.getTransformation();
    }

    @Override
    public ModelItemPropertyOverrideList getItemPropertyOverrides() {
        return source.getItemPropertyOverrides();
    }
}