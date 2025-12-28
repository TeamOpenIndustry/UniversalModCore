package cam72cam.mod.mixin.feat.textfield_click;

import cam72cam.mod.mixin.accessor.AGuiTextField;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Backport of 1.12 method to see if a textfield's click is handled
 */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField implements AGuiTextField {
    @Shadow private boolean canLoseFocus;
    @Shadow private boolean enableBackgroundDrawing;
    @Shadow private boolean isFocused;
    @Shadow @Final private FontRenderer fontRenderer;
    @Shadow private int lineScrollOffset;
    @Shadow private String text;
    @Shadow public int x;
    @Shadow public int y;
    @Shadow public int width;
    @Shadow public int height;
    @Shadow public abstract void setFocused(boolean p_setFocused_1_);
    @Shadow public abstract int getWidth();
    @Shadow public abstract void setCursorPosition(int p_setCursorPosition_1_);

    @Unique
    @Override
    public boolean mouseClicked(int p_mouseClicked_1_, int p_mouseClicked_2_, int p_mouseClicked_3_) {
        boolean lvt_4_1_ = p_mouseClicked_1_ >= this.x && p_mouseClicked_1_ < this.x + this.width && p_mouseClicked_2_ >= this.y && p_mouseClicked_2_ < this.y + this.height;
        if (this.canLoseFocus) {
            this.setFocused(lvt_4_1_);
        }

        if (this.isFocused && lvt_4_1_ && p_mouseClicked_3_ == 0) {
            int lvt_5_1_ = p_mouseClicked_1_ - this.x;
            if (this.enableBackgroundDrawing) {
                lvt_5_1_ -= 4;
            }

            String lvt_6_1_ = this.fontRenderer.trimStringToWidth(this.text.substring(this.lineScrollOffset), this.getWidth());
            this.setCursorPosition(this.fontRenderer.trimStringToWidth(lvt_6_1_, lvt_5_1_).length() + this.lineScrollOffset);
            return true;
        } else {
            return false;
        }
    }
}
