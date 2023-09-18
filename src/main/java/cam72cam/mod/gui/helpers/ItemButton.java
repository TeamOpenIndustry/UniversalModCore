package cam72cam.mod.gui.helpers;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.util.With;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Internal item button class */
public abstract class ItemButton extends AbstractButton {
    public ItemStack stack;

    public ItemButton(ItemStack stack, int x, int y) {
        super(x, y, 32, 32, Component.literal(""));
        this.stack = stack;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(getX(), getY(), getX() + 32, getY() + 32, 0xFFFFFFFF);
        // Pollutes global state...
        // TODO 1.17.1 RenderHelper.turnBackOn();
        Minecraft mc = Minecraft.getInstance();

        Font font = Minecraft.getInstance().font;
        try (With ctx = RenderContext.apply(
                new RenderState().translate(getX(), getY(), 0).scale(2, 2, 1)
        )) {
            graphics.renderItem(stack.internal, 0, 0);
            // TODO 1.20.1 mc.getItemRenderer().renderGuiItemDecorations(new PoseStack(), font, stack.internal, 0, 0);
        }

        // Pollutes global state...
        // TODO 1.17.1 RenderHelper.turnOff();
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX()+ 32 && mouseY >= this.getY() && mouseY < this.getY() + 32;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
