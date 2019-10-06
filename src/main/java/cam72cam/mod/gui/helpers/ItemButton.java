package cam72cam.mod.gui.helpers;

import cam72cam.mod.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import org.lwjgl.opengl.GL11;

public class ItemButton extends AbstractButtonWidget {

    public ItemStack stack;

    public ItemButton(int buttonId, ItemStack stack, int x, int y) {
        super(x, y, 32, 32, "");
        this.stack = stack;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        GUIHelpers.drawRect(x, y, 32, 32, 0xFFFFFFFF);
        //RenderHelper.enableStandardItemLighting();

        MinecraftClient mc = MinecraftClient.getInstance();

        //mc.getRenderItem().renderItemIntoGUI(stack, x, y);
        GL11.glPushMatrix();
        {
            GL11.glTranslated(x, y, 0);
            GL11.glScaled(2, 2, 1);
            mc.getItemRenderer().renderGuiItemIcon(stack.internal, 0, 0);
        }
        GL11.glPopMatrix();
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX < this.x + 32 && mouseY >= this.y && mouseY < this.y + 32;
    }
}
