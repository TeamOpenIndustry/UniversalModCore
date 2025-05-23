package rendertrack.vertex;

import cam72cam.mod.ModCore;
import com.google.common.primitives.UnsignedInteger;
import org.lwjgl.opengl.GL32;

import java.util.List;

/**
 * Wrapper class for OpenGL Vertex Array Object.<br>
 * Holds a reference to the vao in VRAM
 * @author Haxorouse
 */
public class VAO {
    /** I think this is unsigned, but I'm not sure how lwjgl implements it*/
    private int vao; //make these final?
    private VBO vbo;
    private EBO ebo;
    public VertexFormat format;
    private boolean inited = false;

    /**
     * Create an empty VAO with nothing bound to it.<br>
     * Remember to attach a VBO before initing.
     */
    public VAO() {
        vao = GL32.glGenVertexArrays();
    }

    /**
     * Create a new VAO with this VBO attached, but not yet bound in VRAM.
     * @param vbo The vertex buffer for this array object.
     */
    public VAO(VBO vbo) {

    }

    /**
     * Create a new VAO with this VBO and EBO attached, but not yet bound in VRAM.
     * @param vbo The vertex buffer for this array object.
     * @param ebo The element buffer for this array object.
     */
    public VAO(VBO vbo, EBO ebo) {

    }

    /**
     * Create a new VAO with this vertex format attached, but nothing bound.
     * @param format The vertex format for this array object.
     */
    public VAO(VertexFormat format) {

    }

    /**
     * Create a new VAO with this VBO and vertex format attached, but not yet bound in VRAM.
     * @param vbo The vertex buffer for this array object.
     * @param format The vertex format for this array object.
     */
    public VAO(VBO vbo, VertexFormat format) {

    }

    /**
     * Create a new VAO with this VBO, EBO, and vertex format attached, but not yet bound in VRAM.
     * @param vbo The vertex buffer for this array object.
     * @param ebo The element buffer for this array object.
     * @param format The vertex format for this array object.
     */
    public VAO(VBO vbo, EBO ebo, VertexFormat format) {

    }

    /**
     * Binds this VAO and binds the VBO and EBO to it.<br>
     * Saves state and restores automatically, not recommended to call in sequence.<br>
     * Separated from constructor for performance considerations.
     */
    public void init() {
        if(inited) {
            return;
        }
    }

    /**
     * Binds a set of VAOs sequentially to avoid redundant state restores.
     * @param vaos list of VAOs to be inited.
     */
    public static void init(List<VAO> vaos) {
        //save current state

        //init
        for(VAO array : vaos) {
            GL32.glBindVertexArray(array.vao);
            if(array.vbo != null) {
                GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, array.vbo.getVbo());
                array.vbo.init();
                array.vbo.freeRAM();
            } else {
                ModCore.warn("tried to init a VAO with no VBO, don't do this!");
                break;
            }
            if(array.ebo != null) {

            }
            if(array.format != null) {

            } else {
                //default format
            }
        }

        //restore previous state
    }

    /**
     * Function to apply a vertex format to this vao.<br>
     * Only call while this vao is bound
     * @param format vertex format to be applied
     */
    private void applyFormat(VertexFormat format) {

    }

    /**
     * Set the VBO for this VAO if not already set.
     * @param vbo The vertex buffer for this array.
     */
    public void setVbo(VBO vbo) {
        if(vbo != null) {
            this.vbo = vbo;
        } else {
            ModCore.warn("VBO already set, use override method to force");
        }
    }

    /**
     * Set the VBO for this VAO if not already set, able force it if vbo is already set.<br>
     * DON'T USE UNLESS YOU KNOW WHAT YOU'RE DOING!!!!!!
     * @param vbo The vertex buffer for this array.
     * @param override Whether or not to force setting the vbo regardless of if it is already set or not.
     */
    public void setVbo(VBO vbo, boolean override) {
        if(override) {
            this.vbo = vbo;
        } else {
            setVbo(vbo);
        }
    }

    /**
     * Set the EBO for this VAO if not already set.
     * @param ebo
     */
    public void setEbo(EBO ebo) {
        if(ebo != null) {
            this.ebo = ebo;
        } else {
            ModCore.warn("EBO already set, use override method to force");
        }
    }

    /**
     * Set the EBO for this VAO if not already set, able force it if ebo is already set.<br>
     * DON'T USE UNLESS YOU KNOW WHAT YOU'RE DOING!!!!!!
     * @param ebo The element buffer for this array.
     * @param override Whether or not to force setting the ebo regardless of if it is already set or not.
     */
    public void setEbo(EBO ebo, boolean override) {
        if(override) {
            this.ebo = ebo;
        } else {
            setEbo(ebo);
        }
    }

    //public void createVBO

    /**
     * Set the vertex format for this VAO if not already set.
     * @param format Vertex Format to use for this VAO.
     */
    public void setVertexFormat(VertexFormat format) {
        if(format != null) {
            this.format = format;
        } else {
            ModCore.warn("Vertex format already set, use override method to force");
        }
    }

    /**
     * Set the vertex format for this VAO if not already set, able force it if format is already set.<br>
     * DON'T USE UNLESS YOU KNOW WHAT YOU'RE DOING!!!!!!
     * @param format Vertex Format to use for this VAO.
     * @param override Whether or not to force setting the format regardless of if it is already set or not.
     */
    public void setVertexFormat(VertexFormat format, boolean override) {
        if(override){
            this.format = format;
        } else {
            setVertexFormat(format);
        }
    }
}
