package cam72cam.mod.gui_v2.core.actions;

import com.sun.media.jfxmedia.events.PlayerStateEvent;

public interface IUpdatable {
    void postRender();
    void onStateChange();
}
