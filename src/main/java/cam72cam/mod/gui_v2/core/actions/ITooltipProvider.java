package cam72cam.mod.gui_v2.core.actions;

import cam72cam.mod.text.PlayerMessage;

import java.util.List;

public interface ITooltipProvider {
    List<PlayerMessage> getTooltips();
    void setTooltip(List<PlayerMessage> text);
}
