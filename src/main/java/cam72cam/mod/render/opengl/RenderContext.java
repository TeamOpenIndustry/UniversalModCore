package cam72cam.mod.render.opengl;

import cam72cam.mod.ModCore;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.util.With;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL32;

import java.util.*;
import java.util.function.Supplier;

import static cam72cam.mod.render.opengl.Texture.NO_TEXTURE;

public class RenderContext {
    //Lightmap UV coordinate for full bright
    public static final int FULL_BRIGHT = 240;
    public static final MultiBufferSource.BufferSource IMMEDIATE = MultiBufferSource.immediate(new ByteBufferBuilder(16*1024));
    public static final Supplier<String> UMC_DEBUG = () -> "UMC";
    private static final PerspectiveProjectionMatrixBuffer projectionBuffer = new PerspectiveProjectionMatrixBuffer("umc");

    //Modified from rendertype_entity_cutout, fix model normal
    public static RenderPipeline UMC_CORE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
            .withVertexShader(ResourceLocation.fromNamespaceAndPath(ModCore.MODID, "umc_core"))
            .withFragmentShader(ResourceLocation.fromNamespaceAndPath(ModCore.MODID, "umc_core"))
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withSampler("Sampler2")
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
            .withLocation(ResourceLocation.fromNamespaceAndPath(ModCore.MODID, "umc_core"))
            .build();

    //More a holder than renderer for now
    public static RenderType UMC_CORE_RT = RenderType.create("umc_core", 4194304, UMC_CORE,
                                                             RenderType.CompositeState.builder().createCompositeState(false));

    //Cache UMC_CORE variants derived from RenderState (pipeline state is immutable in 1.21.8)
    private static final Map<PipelineKey, RenderPipeline> PIPELINE_CACHE = new HashMap<>();

    public static float lastLightX;
    public static float lastLightY;

    public static ThreadLocal<RenderState> currentState = new ThreadLocal<>();

    private static final List<Runnable> deferredCall = new LinkedList<>();

    private RenderContext() {
    }

    private record PipelineKey(boolean depthTest, boolean cullFace, boolean depthMask, BlendFunction blend) {
    }

    private static RenderPipeline getPipeline(RenderState state) {
        boolean depthTest = state.depth_test == null || state.depth_test;
        boolean cullFace = state.cull_face == null || state.cull_face;
        boolean depthMask = state.depth_mask == null || state.depth_mask;
        BlendFunction blend = state.blend != null && state.blend.enabled ? state.blend.function : null;

        PipelineKey key = new PipelineKey(depthTest, cullFace, depthMask, blend);
        return PIPELINE_CACHE.computeIfAbsent(key, k -> {
            RenderPipeline.Builder builder = UMC_CORE.toBuilder()
                    .withDepthTestFunction(k.depthTest() ? DepthTestFunction.LEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(k.cullFace())
                    .withDepthWrite(k.depthMask());
            if (k.blend() != null) {
                builder.withBlend(k.blend());
            } else {
                builder.withoutBlend();
            }
            return builder.build();
        });
    }

    public static With applyBaseState(RenderState state) {
        List<Runnable> restore = new ArrayList<>();

        if (state.model_view != null) {
            Matrix4f oldModelView = new Matrix4f(RenderSystem.getModelViewMatrix());
            restore.add(() -> RenderSystem.getModelViewMatrix().set(oldModelView));
            Matrix4f target = state.model_view.copy().transpose().convertToMoj();
            RenderSystem.getModelViewMatrix().set(target);
        }

        if (state.projection != null) {
            RenderSystem.backupProjectionMatrix();
            restore.add(RenderSystem::restoreProjectionMatrix);
            Matrix4f target = state.projection.copy().transpose().convertToMoj();
            RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(target), ProjectionType.PERSPECTIVE);
        }

        if (state.texture != null && state.texture != NO_TEXTURE) {
            currentState.set(state);
            //Normal and Specular handled in mixin.feat.iris_pbr
            GpuTextureView view = state.texture.getTexView();
            currentState.remove();

            GpuTextureView oldTexture = RenderSystem.getShaderTexture(0);
            RenderSystem.setShaderTexture(0, view);
            restore.add(() -> RenderSystem.setShaderTexture(0, oldTexture));
        }

        if (state.scissor_test != null && state.scissor_test && state.scissor_range != null) {
            int scaleFactor = Minecraft.getInstance().getWindow().getGuiScale();
            int screenHeight = GUIHelpers.getScreenHeight() * scaleFactor;

            int x = (int) state.scissor_range.getMinX() * scaleFactor;
            int y = (int) state.scissor_range.getMinY() * scaleFactor;
            int width = (int) state.scissor_range.getWidth() * scaleFactor;
            int height = (int) state.scissor_range.getHeight() * scaleFactor;

            //We set origin point at Top-Left corner but OpenGL takes Bottom-Left corner, so wraps y
            RenderSystem.enableScissorForRenderTypeDraws(x, screenHeight - y - height, width, height);
            restore.add(RenderSystem::disableScissorForRenderTypeDraws);
        }

        return () -> restore.forEach(Runnable::run);
    }

    public static With apply(RenderState state) {
        With ctx = applyBaseState(state);

        if (state.stage == Stage.ITEM_SPRITE_TEX) {
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        }

        checkError();
        return ctx;
    }

    @ApiStatus.Internal
    public static With applyAndDraw(MeshData data, RenderState state) {
        With ctx = apply(state);

        if (RenderSystem.getShaderTexture(0) == null) {
            RenderSystem.setShaderTexture(0, Minecraft.getInstance().getTextureManager()
                                                              .getTexture(TextureAtlas.LOCATION_BLOCKS)
                                                              .getTextureView());
        }
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();

        RenderPipeline pipeline = getPipeline(state);

        try {
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                    RenderSystem.getModelViewMatrix(),
                    state.color != null ? new Vector4f(state.color[0], state.color[1], state.color[2], state.color[3])
                                        : new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                    RenderSystem.getModelOffset(),
                    RenderSystem.getTextureMatrix(),
                    RenderSystem.getShaderLineWidth());

            VertexFormat format = pipeline.getVertexFormat();
            GpuBuffer vertexBuffer = format.uploadImmediateVertexBuffer(data.vertexBuffer());

            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;
            if (data.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(data.drawState().mode());
                indexBuffer = sequential.getBuffer(data.drawState().indexCount());
                indexType = sequential.type();
            } else {
                indexBuffer = format.uploadImmediateIndexBuffer(data.indexBuffer());
                indexType = data.drawState().indexType();
            }

            RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
            GpuTextureView color = RenderSystem.outputColorTextureOverride != null
                                   ? RenderSystem.outputColorTextureOverride
                                   : target.getColorTextureView();
            GpuTextureView depth = target.useDepth
                                   ? (RenderSystem.outputDepthTextureOverride != null
                                      ? RenderSystem.outputDepthTextureOverride
                                      : target.getDepthTextureView())
                                   : null;

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                                             .createRenderPass(UMC_DEBUG, color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
                pass.setPipeline(pipeline);

                ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
                if (scissor.enabled()) {
                    pass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
                }

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setVertexBuffer(0, vertexBuffer);

                for (int i = 0; i < 12; i++) {
                    GpuTextureView sampler = RenderSystem.getShaderTexture(i);
                    if (sampler != null) {
                        pass.bindSampler("Sampler" + i, sampler);
                    }
                }

                pass.setIndexBuffer(indexBuffer, indexType);
                pass.drawIndexed(0, 0, data.drawState().indexCount(), 1);
            }
        } finally {
            Minecraft.getInstance().gameRenderer.overlayTexture().teardownOverlayColor();
            Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
            if (data != null) {
                data.close();
            }
            checkError();
        }

        return ctx;
    }

    public static void applyBool(int opt, boolean currState) {
        if (currState) {
            GL32.glEnable(opt);
        } else {
            GL32.glDisable(opt);
        }
    }

    public static void checkError() {
        int err = GL32.glGetError();
        if (err != 0) {
            ModCore.error("We broke something: %s", err);
        }
    }

    public static void addDeferred(Runnable runnable) {
        deferredCall.add(runnable);
    }

    public static boolean hasDeferred() {
        return !deferredCall.isEmpty();
    }

    public static void flushDeferred() {
        deferredCall.forEach(Runnable::run);
        deferredCall.clear();
    }

    public enum Stage {
        BLOCK,

        ENTITY, //Also particles

        ITEM_SPRITE_TEX,
        ITEM_IN_WORLD,
        ITEM_IN_GUI,

        GUI,

        OVERLAY,      //Mouseover...
        OVERLAY_TEXT, //Name plates...

        NONE
    }
}
