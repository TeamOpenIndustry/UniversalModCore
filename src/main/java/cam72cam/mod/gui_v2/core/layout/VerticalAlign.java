package cam72cam.mod.gui_v2.core.layout;

import javax.annotation.Nullable;

public enum VerticalAlign {
    TOP,
    MIDDLE,
    BOTTOM;

    public static @Nullable VerticalAlign parse(String s) {
        switch (s.toUpperCase()) {
            case "TOP":
                return TOP;
            case "MIDDLE":
            case "CENTER":
                return MIDDLE;
            case "BOTTOM":
                return BOTTOM;
        }
        return null;
    }
}
