package cam72cam.mod.gui.helpers;

import cam72cam.mod.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import cam72cam.mod.util.With;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;

/** Internal item button class */
public abstract class ItemButton extends AbstractButton {
    public ItemStack stack;

    public ItemButton(ItemStack stack, int x, int y) {
        super(x, y, 32, 32, new TextComponent(""));
        this.stack = stack;
    }

    @Override
    public void render(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
        GuiComponent.fill(ms, x, y, x + 32, y + 32, 0xFFFFFFFF);
        // Pollutes global state...
        // TODO 1.17.1 RenderHelper.turnBackOn();
        Minecraft mc = Minecraft.getInstance();

        Font font = Minecraft.getInstance().font;
        try (With ctx = RenderContext.apply(
                new RenderState().translate(x, y, 0).scale(2, 2, 1)
        )) {
            mc.getItemRenderer().renderAndDecorateItem(stack.internal(), 0, 0);
            mc.getItemRenderer().renderGuiItemDecorations(font, stack.internal(), 0, 0);
        }

        // Pollutes global state...
        // TODO 1.17.1 RenderHelper.turnOff();
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX < this.x + 32 && mouseY >= this.y && mouseY < this.y + 32;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
