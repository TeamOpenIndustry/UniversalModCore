package cam72cam.mod.mixin.accessor;

import net.minecraft.client.gui.GuiTextField;

/**
 * Accessor for GuiTextField
 * @see cam72cam.mod.mixin.feat.textfield_click.MixinGuiTextField
 */
public interface AGuiTextField {
    static AGuiTextField from(GuiTextField x) {
        return (AGuiTextField) x;
    }

    boolean mouseClicked(int p_mouseClicked_1_, int p_mouseClicked_2_, int p_mouseClicked_3_);
}
