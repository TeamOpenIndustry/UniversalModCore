package cam72cam.mod.gui.helpers;

import cam72cam.mod.gui.Slider;

public class GuiScrollBar extends Slider.HookedSliderWidget {
    private final double min;
    private final double max;

    public GuiScrollBar(int xPos, int yPos, int width, int height, String displayStr, double min, double max, double start) {
        super(xPos, yPos, width, height, (start - min) / (max - min));
        this.min = min;
        this.max = max;
        super.setMessage(displayStr);
    }

    @Override
    protected void onDrag(double double_1, double double_2, double double_3, double double_4) {
        this.value = (double_2 - (this.y + 4)) / (float) (this.height - 8);

        /* TODO
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(this.x, this.y + (int) (this.sliderValue * (float) (this.height - 8)), 0, 66, 20, 20);
        */
    }

    @Override
    public void onClick(double double_1, double double_2) {
        this.value = (float) (double_2 - (this.y + 4)) / (float) (this.height - 8);
    }
}
