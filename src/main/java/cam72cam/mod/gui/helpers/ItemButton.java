package cam72cam.mod.gui.helpers;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.OpenGL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;

/** Internal item button class */
class ItemButton extends GuiButton {

    public ItemStack stack;

    public ItemButton(int buttonId, ItemStack stack, int x, int y) {
        super(buttonId, x, y, 32, 32, "");
        this.stack = stack;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(x, y, x + 32, y + 32, 0xFFFFFFFF);
        // Pollutes global state...
        RenderHelper.enableGUIStandardItemLighting();

        FontRenderer font = stack.internal.getItem().getFontRenderer(stack.internal);
        OpenGL.RenderContext ctx = new OpenGL.RenderContext();
        ctx.translate(x, y, 0);
        ctx.scale(2, 2, 1);
        try (OpenGL.RenderContext matrix = ctx.apply()) {
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack.internal, 0, 0);
            mc.getRenderItem().renderItemOverlays(font, stack.internal, 0, 0);
        }

        // Pollutes global state...
        RenderHelper.disableStandardItemLighting();
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX < this.x + 32 && mouseY >= this.y && mouseY < this.y + 32;
    }
}
