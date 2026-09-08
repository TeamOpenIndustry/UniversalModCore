package cam72cam.mod.render.opengl;

import cam72cam.mod.ModCore;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.mixin.accessor.ARenderPass;
import cam72cam.mod.util.With;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.config.NeoForgeClientConfig;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL32;

import java.nio.IntBuffer;
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
    public static RenderPipeline UMC_CORE = RenderPipeline.builder()
            .withVertexShader(ResourceLocation.fromNamespaceAndPath(ModCore.MODID, "umc_core"))
            .withFragmentShader(ResourceLocation.fromNamespaceAndPath(ModCore.MODID, "umc_core"))
            .withLocation(ResourceLocation.fromNamespaceAndPath(ModCore.MODID, "umc_core"))
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS).build();

    //More a holder than renderer for now
    public static RenderType UMC_CORE_RT = RenderType.create("umc_core", 4194304, UMC_CORE,
                                                             RenderType.CompositeState.builder().createCompositeState(false));

    private static IntBuffer fourIntBuffer;

    public static float lastLightX;
    public static float lastLightY;

    public static ThreadLocal<RenderState> currentState = new ThreadLocal<>();

    private static final List<Runnable> deferredCall = new LinkedList<>();

    private RenderContext() {
    }

    public static With applyBaseState(RenderState state) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        RenderPipeline.Builder builder = RenderPipeline.builder();
        OptionalInt color;
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

        GpuTextureView view;
        if (state.texture != NO_TEXTURE && state.texture != null) {
            currentState.set(state);
            //Normal and Specular handled in mixin.feat.iris_pbr
            view = state.texture.getTexView();
            RenderSystem.setShaderTexture(0, view);
            currentState.remove();
        } else {
            view = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        }

        {
            if (state.color == null) {
                color = OptionalInt.of(0xFFFFFFFF);
            } else {
                color = OptionalInt.of((int) (state.color[0] * 256) << 24
                                             | (int) (state.color[1] * 256) << 16
                                             | (int) (state.color[2] * 256) << 8
                                             | (int) (state.color[3] * 256));
            }
        }
        RenderPass pass = encoder.createRenderPass(UMC_DEBUG, view, color);

        if (state.depth_test != null) {
            if(state.depth_test) {
                builder.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST);
            } else {
                builder.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
            }
        }

        if (state.cull_face != null) {
            builder.withCull(state.cull_face);
        }

        if (state.depth_mask != null) {
            builder.withDepthWrite(state.depth_mask);
        }

        if (state.blend != null) {
            if (state.blend.enabled) {
                builder.withBlend(state.blend.function);
            } else {
                builder.withoutBlend();
            }
        }

        if (state.scissor_test != null) {
            if (state.scissor_test && state.scissor_range != null) {
                int scaleFactor = Minecraft.getInstance().getWindow().getGuiScale();
                int screenHeight = GUIHelpers.getScreenHeight() * scaleFactor;

                int x = (int) state.scissor_range.getMinX() * scaleFactor;
                int y = (int) state.scissor_range.getMinY() * scaleFactor;
                int width = (int) state.scissor_range.getWidth() * scaleFactor;
                int height = (int) state.scissor_range.getHeight() * scaleFactor;

                //We set origin point at Top-Left corner but OpenGL takes Bottom-Left corner, so wraps y
                pass.enableScissor(x, screenHeight - y - height, width, height);
            }
        }
        RenderContext.checkError();

        pass.setPipeline(builder.build());

        return new With() {
            @Override
            public void restore() {
                pass.close();
            }

            @Override
            public RenderPass getContent() {
                return pass;
            }
        }.and(() -> restore.forEach(Runnable::run));
    }

    public static With apply(RenderState state) {
        RenderPass pass = applyBaseState(state).getContent();
        List<Runnable> restore = new ArrayList<>();

        if (state.lightmap != null) {
            //Our custom shader will handle vanilla emissive stuff
            float oldX;
            float oldY;
            if (state.stage == Stage.ENTITY) {
                oldX = lastLightX;
                oldY = lastLightY;
            } else {
//                oldX = GlStateManager.lastBrightnessX;
//                oldY = GlStateManager.lastBrightnessY;
                //TODO Add our own tracer
                oldX = 1;
                oldY = 1;
            }
            setupLightMap(ARenderPass.from(pass).getRenderPipeline(), state, state.lightmap[0], state.lightmap[1]);
            restore.add(() -> {
                setupLightMap(ARenderPass.from(pass).getRenderPipeline(), state, oldX, oldY);
            });
        }

        if (state.stage == Stage.ITEM_SPRITE_TEX) {
            //TODO Still necessary?
//            Matrix4f matrix4 = new Matrix4().rotate(Math.toRadians(90), 0, 1, 0).convertToMoj();
//            Vector4f transformed0 = matrix4.transform(new Vector4f(Lighting.DIFFUSE_LIGHT_0, 1));
//            Vector4f transformed1 = matrix4.transform(new Vector4f(Lighting.DIFFUSE_LIGHT_1, 1));
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        }

        checkError();
        return new With() {
            @Override
            public void restore() {
                pass.close();
            }

            @Override
            public RenderPass getContent() {
                return pass;
            }
        }.and(() -> restore.forEach(Runnable::run));
    }

    private static void setupLightMap(RenderPipeline pipeline, RenderState state, float oldX, float oldY) {
        List<VertexFormatElement> elements = pipeline.getVertexFormat().getElements();
        for (int i = 0; i < elements.size(); i++) {
            VertexFormatElement element = elements.get(i);
            if (element.usage() == VertexFormatElement.Usage.UV) {
                for (Map.Entry<String, VertexFormatElement> entry : pipeline.getVertexFormat().getElementMapping().entrySet()) {
                    if (entry.getValue() == element && entry.getKey().equals("UV2")) {
                        GL32.glDisableVertexAttribArray(i);
                        //240 means full bright
                        int x = RenderContext.FULL_BRIGHT;
                        int y = RenderContext.FULL_BRIGHT;
                        if (state.lightmap != null) {
                            x = (int) (state.lightmap[0] * RenderContext.FULL_BRIGHT);
                            y = (int) (state.lightmap[1] * RenderContext.FULL_BRIGHT);
                        }
                        GL32.glVertexAttribI2i(i, x, y);
                    }
                }
            }
        }
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
