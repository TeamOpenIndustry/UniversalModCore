package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.rendering.GUIRenderer;
import cam72cam.mod.gui_v2.control.AbstractButton;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class Button<T extends Button<T>> extends AbstractButton<Button<T>> {
    private static final Identifier VANILLA_BUTTON = new Identifier("textures/gui/control.png");

    /** Custom width/height */
    public Button(int width, int height, PlayerMessage name, BiConsumer<Player.Hand, Button<T>> handler) {
        super(0, 0, width, height, name, handler);
        setEnabled(true);
    }

    @Override
    public void render(GUIRenderer renderer) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        boolean isHovering = isHovering();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        int i = !isEnabled()
                ? 0
                : isHovering ? 2 : 1;
        renderer.texturedRect(VANILLA_BUTTON, this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
        renderer.texturedRect(VANILLA_BUTTON, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
        int j = 14737632;

        if (nameColor != 0) {
            j = nameColor;
        } else if (!this.enabled) {
            j = 10526880;
        } else if (isHovering) {
            j = 16777120;
        }

        renderer.drawCenteredString(this.name.internal.getFormattedText(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j);
    }

    @Override
    public void renderBackground(GUIRenderer renderer) {

    }

    @Override
    public void renderForeground(GUIRenderer renderer) {

    }

    @Override
    public List<PlayerMessage> getTooltips() {
        return Collections.singletonList(this.getName());
    }
}
