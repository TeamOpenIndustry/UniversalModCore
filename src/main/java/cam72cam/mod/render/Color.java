package cam72cam.mod.render;


/** Wrapper for MC's Colors */
public enum Color {
    WHITE(15),
    ORANGE(14),
    MAGENTA(13),
    LIGHT_BLUE(12),
    YELLOW(11),
    LIME(10),
    PINK(9),
    GRAY(8),
    SILVER(7),
    CYAN(6),
    PURPLE(5),
    BLUE(4),
    BROWN(3),
    GREEN(2),
    RED(1),
    BLACK(0),
    ;

    /** Internal, do not use */
    public final int internal;

    Color(int internal) {
        this.internal = internal;
    }
}