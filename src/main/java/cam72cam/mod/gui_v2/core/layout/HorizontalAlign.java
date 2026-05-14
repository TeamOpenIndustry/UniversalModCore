package cam72cam.mod.gui_v2.core.layout;

import javax.annotation.Nullable;

public enum HorizontalAlign {
    LEFT,
    MIDDLE,
    RIGHT;

    public static @Nullable HorizontalAlign parse(String s) {
        switch (s.toUpperCase()) {
            case "LEFT":
                return LEFT;
            case "MIDDLE":
            case "CENTER":
                return MIDDLE;
            case "RIGHT":
                return RIGHT;
        }
        return null;
    }
}
