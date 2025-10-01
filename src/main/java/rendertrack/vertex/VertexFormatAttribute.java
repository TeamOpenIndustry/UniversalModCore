package rendertrack.vertex;

import cam72cam.mod.ModCore;
import org.lwjgl.opengl.GL32;

/**
 * Attribute of vertex data to be assembled into ordered formats.
 * @author Haxorouse
 */
public class VertexFormatAttribute {
    //protected final String name;//TODO javadoc
    /**Index of the attribute in the shader program. *///TODO reword this?
    protected final int location;
    /**Number of the given GLType composing this attribute.<br>
     * ie: 3 floats to define 3D position.<br>
     * Set as 0 to use BGRA, type must be ubyte and int handling normalized to use this size.*/
    protected final int size;
    /**Data type of the attribute.<br>
     * ie: int, float, double. */
    protected final GlType type;
    /**How to handle integer type arrays. */
    protected final IntHandling handling;
    protected final boolean normalize;
    /**Total size of the attribute in bytes. */
    protected final int stride;

    /**
     * Create a new vertex attribute.
     * FOR EXTERNAL MODS: only create attributes in the VertexAttributes hashmap, do not store them in your mod.
     * @param location Index of the attribute in shader.
     * @param size Number of values stored by the attribute, not size in bytes.<br>
     *             Use 0 to indicate use of BGRA as size, type must be ubyte, and int handling must be normalized.
     * @param type Data type of the value.
     * @param handling How to handle int values in float math.
     */
    public VertexFormatAttribute(int location, int size, GlType type, IntHandling handling) {//TODO supported sizes check
        //this.name = name;
        this.location = location;
        this.type = type;
        this.handling = handling;
        this.normalize = handling == IntHandling.NORMALIZE;
        if (size == 0){
            if(!normalize && type != GlType.UBYTE){
                ModCore.error("Invalid use of BGRA size, this will crash if you apply it.");
            }
            this.size = GL32.GL_BGRA;
            this.stride = 4 * type.size;
        } else {
            this.size = size;
            this.stride = size * type.size;
        }
    }

    /**
     * Apply this with glVertexAttribPointer or glVertexAttribIPointer with the offset provided by the vertex format.
     */
    protected void apply(int offset) {
        if(handling == IntHandling.PURE_INT) {
            GL32.glVertexAttribIPointer(location, size, type.glType, stride, offset);
            GL32.glEnableVertexAttribArray(location);
        } else {
            GL32.glVertexAttribPointer(location, size, type.glType, normalize, stride, offset);
            GL32.glEnableVertexAttribArray(location);
        }
    }

    //TODO do we need these?
    public int getLocation() {
        return location;
    }

    public int getSize() {
        return size;
    }

    public GlType getType() {
        return type;
    }

    public boolean isNormalized() {
        return normalize;
    }

    public int getStride() {
        return stride;
    }

    /**
     *
     */
    public static enum GlType {
        BYTE(1, GL32.GL_BYTE, "Byte", true),
        UBYTE(1, GL32.GL_UNSIGNED_BYTE, "Unsigned Byte", true),
        SHORT(2, GL32.GL_SHORT, "Short", true),
        USHORT(2, GL32.GL_UNSIGNED_SHORT, "Unsigned Short", true),
        INT(4, GL32.GL_INT, "Int", true),
        UINT(4, GL32.GL_UNSIGNED_INT, "Unsigned Int", true),
        HALF_FLOAT(2, GL32.GL_HALF_FLOAT, "Half Float", false),
        FLOAT(4, GL32.GL_FLOAT, "Float", false),
        DOUBLE(8, GL32.GL_DOUBLE, "Double", false);


        /**The size of the type in bytes. */
        private final int size;
        private final int glType;
        private final String name;
        /**Whether or not the type is an int that must be normalized for a float pointer. */
        private final boolean isInt;

        private GlType(int size, int glType, String name, boolean isInt) {
            this.size = size;
            this.glType = glType;
            this.name = name;
            this.isInt = isInt;
        }

        public int getSize() {
            return size;
        }

        public int getGlType() {
            return glType;
        }

        public String getName() {
            return name;
        }

        public boolean isInt() {
            return isInt;
        }
    }
    /**Default location of the attributes within the shader for compatibility with minecraft. */
    public static enum DefaultLocation {
        POSITION,
        NORMAL,
        COLOR,
        UV,
        PADDING,
        GENERIC;
    }

    /**OGL Core Profile specification section 2.8 p28-29 */
    public static enum IntHandling {
        /**Normalize int type to [0,1]unsigned, or [-1,1]signed. */
        NORMALIZE,
        /**Convert value directly to float without normalizing to screen space. */
        CONVERT_FLOAT,
        /**Leave type as integer. Uses VertexAttribIPointer instead of VertexAttribPointer. */
        PURE_INT;
    }
}
