package cam72cam.mod.render.opengl;

import cam72cam.mod.ModCore;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.util.With;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import util.Matrix4;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.*;

import static cam72cam.mod.render.opengl.Texture.NO_TEXTURE;

public class RenderContext {
    //Lightmap UV coordinate for full bright
    public static final int FULL_BRIGHT = 240;
    public static final MultiBufferSource.BufferSource IMMEDIATE = MultiBufferSource.immediate(new ByteBufferBuilder(16*1024));

    //Modified from rendertype_entity_cutout, fix model normal
    public static ShaderProgram UMC_CORE = new ShaderProgram(ResourceLocation.fromNamespaceAndPath(ModCore.MODID, "umc_core"),
                                                             DefaultVertexFormat.NEW_ENTITY,
                                                             ShaderDefines.EMPTY);
    //More a holder than renderer for now
    public static RenderType UMC_CORE_RT = RenderType.create("umc_core",
                                                             DefaultVertexFormat.NEW_ENTITY,
                                                             VertexFormat.Mode.TRIANGLES,
                                                             GL11.GL_2D,
                                                             RenderType.CompositeState.builder().createCompositeState(false));

    private static IntBuffer fourIntBuffer;

    public static float lastLightX;
    public static float lastLightY;

    public static ThreadLocal<RenderState> currentState = new ThreadLocal<>();

    private static final List<Runnable> deferredCall = new LinkedList<>();

    private RenderContext() {
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
            Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            restore.add(() -> RenderSystem.getProjectionMatrix().set(oldProjection));
            Matrix4f target = state.projection.copy().transpose().convertToMoj();
            RenderSystem.getProjectionMatrix().set(target);
        }

        if (state.texture != NO_TEXTURE && state.texture != null) {
            currentState.set(state);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.texture.getId());
            //Normal and Specular handled in mixin.feat.iris_pbr
            //TODO create handler for OptiFine?
            int oldTexture = RenderSystem.getShaderTexture(0);
            restore.add(() -> RenderSystem.setShaderTexture(0, oldTexture));
            RenderSystem.setShaderTexture(0, state.texture.getId());
            currentState.remove();
        }

        {
            float[] color = state.color;
            if (color == null) {
                color = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
            }
            float[] oldColor = Arrays.copyOf(RenderSystem.getShaderColor(), 4);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            restore.add(() -> RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]));
        }

//        if (state.lighting != null) {
//            boolean oldValue = GL11.glGetBoolean(GL11.GL_LIGHTING);
//            applyBool(GL11.GL_LIGHTING, state.lighting);
//            restore.add(() -> applyBool(GL11.GL_LIGHTING, oldValue));
//        }
//
//        if (state.alpha_test != null) {
//            boolean oldValue = GL11.glGetBoolean(GL11.GL_ALPHA_TEST);
//            applyBool(GL11.GL_ALPHA_TEST, state.alpha_test);
//            restore.add(() -> applyBool(GL11.GL_ALPHA_TEST, oldValue));
//        }

        if (state.depth_test != null) {
            boolean oldState = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
            if(state.depth_test) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
            } else {
                RenderSystem.disableDepthTest();
            }
            restore.add(() -> {
                if(oldState) {
                    RenderSystem.enableDepthTest();
                } else {
                    RenderSystem.disableDepthTest();
                }
            });
        }

//        if (state.rescale_normal != null) {
//            boolean oldValue = GL11.glGetBoolean(GL12.GL_RESCALE_NORMAL);
//            applyBool(GL12.GL_RESCALE_NORMAL, state.rescale_normal);
//            restore.add(() -> applyBool(GL12.GL_RESCALE_NORMAL, oldValue));
//        }

        if (state.cull_face != null) {
            boolean oldState = GL11.glGetBoolean(GL11.GL_CULL_FACE);
            if(state.cull_face) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
            restore.add(() -> {
                if(oldState) {
                    RenderSystem.enableCull();
                } else {
                    RenderSystem.disableCull();
                }
            });
        }

        if (state.depth_mask != null) {
            RenderSystem.depthMask(state.depth_mask);
            restore.add(() -> RenderSystem.depthMask(true));
        }

        if (state.blend != null) {
            restore.add(state.blend.apply());
        }

        if (state.scissor_test != null) {
            boolean oldValue = GL11.glGetBoolean(GL11.GL_SCISSOR_TEST);
            applyBool(GL11.GL_SCISSOR_TEST, state.scissor_test);
            if (state.scissor_test && state.scissor_range != null) {
                int scaleFactor = (int) Minecraft.getInstance().getWindow().getGuiScale();
                int screenHeight = GUIHelpers.getScreenHeight() * scaleFactor;

                int x = (int) state.scissor_range.getMinX() * scaleFactor;
                int y = (int) state.scissor_range.getMinY() * scaleFactor;
                int width = (int) state.scissor_range.getWidth() * scaleFactor;
                int height = (int) state.scissor_range.getHeight() * scaleFactor;

                if (fourIntBuffer == null) {
                    //16 ints in case it overflows...
                    fourIntBuffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer();
                }
                fourIntBuffer.position(0);
                GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, fourIntBuffer);
                int[] oldScissor = new int[]{fourIntBuffer.get(0), fourIntBuffer.get(1), fourIntBuffer.get(2), fourIntBuffer.get(3)};
                restore.add(() -> GL11.glScissor(oldScissor[0], oldScissor[1], oldScissor[2], oldScissor[3]));

                //We set origin point at Top-Left corner but OpenGL takes Bottom-Left corner, so wraps y
                GL11.glScissor(x, screenHeight - y - height, width, height);
            }
            restore.add(() -> applyBool(GL11.GL_SCISSOR_TEST, oldValue));
        }
        RenderContext.checkError();

        return () -> restore.forEach(Runnable::run);
    }

    public static With apply(RenderState state) {
        With ctx = applyBaseState(state);
        List<Runnable> restore = new ArrayList<>();
        CompiledShaderProgram shader = RenderSystem.getShader();

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
            setupLightMap(shader, state.lightmap[0], state.lightmap[1]);
            restore.add(() -> {
                setupLightMap(shader, oldX, oldY);
            });
        }

        if (state.stage == Stage.ITEM_SPRITE_TEX) {
            Matrix4f matrix4 = new Matrix4().rotate(Math.toRadians(90), 0, 1, 0).convertToMoj();
//            Lighting.setupLevel(matrix4.convertToMoj());
            Vector4f transformed0 = matrix4.transform(new Vector4f(Lighting.DIFFUSE_LIGHT_0, 1));
            Vector4f transformed1 = matrix4.transform(new Vector4f(Lighting.DIFFUSE_LIGHT_1, 1));
            RenderSystem.setShaderLights(new Vector3f(transformed0.x(), transformed0.y(), transformed0.z()),
                                         new Vector3f(transformed1.x(), transformed1.y(), transformed1.z()));
        }

        applyShaderFields(shader);

        shader.apply();
        restore.add(shader::clear);
        checkError();
        return ctx.and(() -> restore.forEach(Runnable::run));
    }

    private static void applyShaderFields(CompiledShaderProgram shader) {
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        }

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        }

        if (shader.TEXTURE_MATRIX != null) {
            shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (shader.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set((float)window.getWidth(), (float)window.getHeight());
        }

        if (shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }

        if (shader.GLINT_ALPHA != null) {
            shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }

        for (int i = 0; i < 8; ++i) {
            int o = RenderSystem.getShaderTexture(i);
            shader.bindSampler("Sampler" + i, o);
        }

        FogParameters fogparameters = RenderSystem.getShaderFog();
        if (shader.FOG_START != null) {
            shader.FOG_START.set(fogparameters.start());
        }

        if (shader.FOG_END != null) {
            shader.FOG_END.set(fogparameters.end());
        }

        if (shader.FOG_COLOR != null) {
            shader.FOG_COLOR.set(fogparameters.red(), fogparameters.green(), fogparameters.blue(), fogparameters.alpha());
        }

        if (shader.FOG_SHAPE != null) {
            shader.FOG_SHAPE.set(fogparameters.shape().getIndex());
        }

        if (shader.GAME_TIME != null) {
            shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }

        RenderSystem.setupShaderLights(shader);
    }

    //Note: with Iris sometimes we get corrupted light texture (32*32, tinted brown)
    private static void setupLightMap(CompiledShaderProgram shader, float oldX, float oldY) {
        int uv2Binding = GL20.glGetAttribLocation(shader.getProgramId(), "UV2");
        if (uv2Binding != -1) {
            //240 means full bright
            int x = (int) (oldX * RenderContext.FULL_BRIGHT);
            int y = (int) (oldY * RenderContext.FULL_BRIGHT);
            GL32.glVertexAttribI2i(uv2Binding, x, y);
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
