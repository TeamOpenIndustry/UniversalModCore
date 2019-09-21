package cam72cam.mod.gui.helpers;

import cam72cam.mod.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.opengl.GL11;

public class ItemButton extends GuiButton {

    public ItemStack stack;

    public ItemButton(int buttonId, ItemStack stack, int x, int y) {
        super(buttonId, x, y, 32, 32, "");
        this.stack = stack;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        Gui.drawRect(xPosition, yPosition, xPosition + 32, yPosition + 32, 0xFFFFFFFF);
        RenderHelper.enableStandardItemLighting();

        FontRenderer font = stack.internal.getItem().getFontRenderer(stack.internal);
        if (font == null) {
            font = mc.fontRendererObj;
        }
        //mc.getRenderItem().renderItemIntoGUI(stack, x, y);
        GL11.glPushMatrix();
        {
            GL11.glTranslated(xPosition, yPosition, 0);
            GL11.glScaled(2, 2, 1);
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack.internal, 0, 0);
            mc.getRenderItem().renderItemOverlays(font, stack.internal, 0, 0);
        }
        GL11.glPopMatrix();
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.xPosition && mouseX < this.xPosition + 32 && mouseY >= this.yPosition && mouseY < this.yPosition + 32;
    }
}
