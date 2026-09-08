package cam72cam.mod.render;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import cam72cam.mod.render.cutter.MeshPlaneCutter;
import cam72cam.mod.math.Plane;
import cam72cam.mod.render.cutter.BakedQuadAdapter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
class BakedScaledModel {
    // I know this is evil and I love it :D

    private final Matrix4 transform;
    private final BlockStateModel source;
    private final boolean isCut;
    private final Map<Direction, List<BakedQuad>> quadCache = new HashMap<>();

    private final RandomSource quadRand = RandomSource.create();

    public BakedScaledModel(BlockStateModel source, Matrix4 transform) {
        this.source = source;
        this.transform = transform;
        this.isCut = false;
    }

    public BakedScaledModel(BlockStateModel source, float height) {
        this.source = source;
        transform = new Matrix4().scale(1, height, 1);
        this.isCut = false;
    }

    public BakedScaledModel(BlockStateModel source, Matrix4 transform, Plane plane) {
        this.source = source;
        this.transform = transform;
        this.isCut = true;

        quadCache.put(null, new ArrayList<>());
        for (Direction dir : Direction.values()) {
            quadCache.put(dir, new ArrayList<>());
        }

        List<BlockModelPart> parts = source.collectParts(quadRand);

        List<BakedQuad> all = new ArrayList<>(parts.stream().flatMap(part -> part.getQuads(null).stream()).toList());
        for (Direction dir : Direction.values()) {
            all.addAll(parts.stream().flatMap(part -> part.getQuads(dir).stream()).toList());
        }

        all = MeshPlaneCutter.cut(transformQuads(all), plane, new BakedQuadAdapter());
        for (BakedQuad quad : all) {
            quadCache.get(null).add(quad);
            quadCache.get(quad.direction()).add(quad);
        }
    }

    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
        if(isCut) {
            return quadCache.getOrDefault(side, Collections.emptyList());
        }

        List<BlockModelPart> parts = source.collectParts(quadRand);
        if (quadCache.get(side) == null) {
            quadCache.put(side, transformQuads(parts.stream().flatMap(part -> part.getQuads(null).stream()).toList()));
        }

        return quadCache.get(side);
    }

    private List<BakedQuad> transformQuads(List<BakedQuad> quads) {
        Matrix4f mat = transform.convertToMoj();
        return QuadTransformers.applying(new Transformation(mat)).process(quads);
    }

    public boolean useAmbientOcclusion() {
        return source.collectParts(quadRand).stream().anyMatch(q -> q.ambientOcclusion().toBoolean(false));
    }

    public TextureAtlasSprite getParticleIcon() {
        return source.collectParts(quadRand).getFirst().particleIcon();
    }
}