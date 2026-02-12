package cam72cam.mod.render.opengl;

import cam72cam.mod.ModCore;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.render.ShaderHelper;
import cam72cam.mod.util.With;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import util.Matrix4;

import java.util.*;

import static cam72cam.mod.render.opengl.Texture.NO_TEXTURE;

public class RenderContext {
    //Lightmap UV coordinate for full bright
    public static final int FULL_BRIGHT = 240;

    //Modified from rendertype_entity_cutout, fix model normal
    public static ShaderInstance UMC_CORE;

    public static float lastLightX;
    public static float lastLightY;

    public static ThreadLocal<RenderState> currentState = new ThreadLocal<>();

    private static final List<Runnable> deferredCall = new LinkedList<>();

    private RenderContext() {
    }

    public static With apply(RenderState state) {
        RenderContext.checkError();
        List<Runnable> restore = new ArrayList<>();

        ShaderInstance shader = RenderSystem.getShader();
        if (state.model_view != null) {
            Matrix4f oldModelView = RenderSystem.getModelViewMatrix().copy();
            restore.add(() -> RenderSystem.getModelViewMatrix().load(oldModelView));
            Matrix4f target = state.model_view.convertToMoj();
            RenderSystem.getModelViewMatrix().load(target);
        }

        if (state.projection != null) {
            Matrix4f oldProjection = RenderSystem.getProjectionMatrix().copy();
            restore.add(() -> RenderSystem.getProjectionMatrix().load(oldProjection));
            Matrix4f target = state.projection.convertToMoj();
            RenderSystem.getProjectionMatrix().load(target);
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

        if (state.lightmap != null) {
            //Our custom shader will handle vanilla emissive stuff
            float oldX;
            float oldY;
            if (state.stage == Stage.ENTITY) {
                oldX = lastLightX;
                oldY = lastLightY;
            } else {
                oldX = GlStateManager.lastBrightnessX;
                oldY = GlStateManager.lastBrightnessY;
            }
            setupLightMap(shader, state.lightmap[0], state.lightmap[1]);
            restore.add(() -> setupLightMap(shader, oldX, oldY));
        }

        if (state.bools.containsKey(GL11.GL_CULL_FACE)) {
            boolean olcState = GL11.glGetBoolean(GL11.GL_CULL_FACE);
            if(state.bools.get(GL11.GL_CULL_FACE)) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
            restore.add(() -> {
                if(olcState) {
                    RenderSystem.enableCull();
                } else {
                    RenderSystem.disableCull();
                }
            });
        }

        if (state.bools.containsKey(GL11.GL_DEPTH_TEST)) {
            boolean olcState = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
            if(state.bools.get(GL11.GL_DEPTH_TEST)) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
            } else {
                RenderSystem.disableDepthTest();
            }
            restore.add(() -> {
                if(olcState) {
                    RenderSystem.enableDepthTest();
                } else {
                    RenderSystem.disableDepthTest();
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

        if(state.scissorRange != null){
            int scaleFactor = (int) Minecraft.getInstance().getWindow().getGuiScale();
            int screenHeight = GUIHelpers.getScreenHeight() * scaleFactor;

            int x = (int) state.scissorRange.getMinX() * scaleFactor;
            int y = (int) state.scissorRange.getMinY() * scaleFactor;
            int width = (int) state.scissorRange.getWidth() * scaleFactor;
            int height = (int) state.scissorRange.getHeight() * scaleFactor;

            //We set origin point at Top-Left corner but OpenGL takes Bottom-Left corner, so wraps y
            RenderSystem.enableScissor(x, screenHeight - y - height, width, height);
            restore.add(RenderSystem::disableScissor);
        }

        if (state.stage == Stage.ITEM_SPRITE_TEX) {
            Matrix4 matrix4 = new Matrix4();
            matrix4.rotate(Math.toRadians(90), 0, 1, 0);
            Lighting.setupLevel(matrix4.convertToMoj());
        }

        applyShaderFields(shader);

        shader.apply();
        restore.add(shader::clear);
        checkError();
        return () -> restore.forEach(Runnable::run);
    }

    private static void applyShaderFields(ShaderInstance shader) {
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        }

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        }

        if (shader.INVERSE_VIEW_ROTATION_MATRIX != null) {
            shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
        }

        if (shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }

        for (int i = 0; i < 8; ++i) {
            int o = RenderSystem.getShaderTexture(i);
            shader.setSampler("Sampler" + i, o);
        }

        if (shader.FOG_START != null) {
            shader.FOG_START.set(RenderSystem.getShaderFogStart());
        }

        if (shader.FOG_END != null) {
            shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        }

        if (shader.FOG_COLOR != null) {
            shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }

        if (shader.FOG_SHAPE != null) {
            shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        }

        if (shader.TEXTURE_MATRIX != null) {
            shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (shader.GAME_TIME != null) {
            shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }

        if (shader.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set((float)window.getWidth(), (float)window.getHeight());
        }

        RenderSystem.setupShaderLights(shader);
    }

    private static void setupLightMap(ShaderInstance shader, float oldX, float oldY) {
        ImmutableList<VertexFormatElement> elements1 = shader.getVertexFormat().getElements();
        for (int i = 0; i < elements1.size(); i++) {
            VertexFormatElement element = elements1.get(i);
            if (element.getUsage() == VertexFormatElement.Usage.UV) {
                for (Map.Entry<String, VertexFormatElement> entry : shader.getVertexFormat().getElementMapping().entrySet()) {
                    if (entry.getValue() == element && entry.getKey().equals("UV2")) {
                        //240 means full bright
                        int x = (int) (oldX * RenderContext.FULL_BRIGHT);
                        int y = (int) (oldY * RenderContext.FULL_BRIGHT);
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
