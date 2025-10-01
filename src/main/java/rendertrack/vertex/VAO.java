package rendertrack.vertex;

import cam72cam.mod.ModCore;
import com.google.common.primitives.UnsignedInteger;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL32;

import java.util.List;

/**
 * Wrapper class for OpenGL Vertex Array Object.<br>
 * Holds a reference to the vao in VRAM
 * @author Haxorouse
 */
public class VAO {
    /** I think this is unsigned, but I'm not sure how lwjgl implements it*/
    private final int vao; //make these final?
    private VBO vbo;
    private EBO ebo;
    public VertexFormat format;
    private boolean inited = false;
    //TODO implement VAO constructors
    //TODO investigate taking int references instead of always creating the vao here
    /**
     * Create an empty VAO with nothing bound to it.<br>
     * Remember to attach a VBO before initing.
     */
    public VAO() {
        vao = GL32.glGenVertexArrays();
    }
    //TODO descriptions might be wrong, I don't know when buffer transfers happen
    /**
     * Create a new VAO with this VBO attached, but not yet bound in GL server RAM.
     * @param vbo The vertex buffer for this array object.
     */
    public VAO(VBO vbo) {
        vao = GL32.glGenVertexArrays();
        this.vbo = vbo;
    }

    /**
     * Create a new VAO with this VBO and EBO attached, but not yet bound in GL server RAM.
     * @param vbo The vertex buffer for this array object.
     * @param ebo The element buffer for this array object.
     */
    public VAO(VBO vbo, EBO ebo) {
        vao = GL32.glGenVertexArrays();
        this.vbo = vbo;
        this.ebo = ebo;
    }

    /**
     * Create a new VAO with this vertex format attached, but nothing bound.
     * @param format The vertex format for this array object.
     */
    public VAO(VertexFormat format) {
        vao = GL32.glGenVertexArrays();
        this.format = format;
    }

    /**
     * Create a new VAO with this VBO and vertex format attached, but not yet bound in GL server RAM.
     * @param vbo The vertex buffer for this array object.
     * @param format The vertex format for this array object.
     */
    public VAO(VBO vbo, VertexFormat format) {
        vao = GL32.glGenVertexArrays();
        this.format = format;
        this.vbo = vbo;
    }

    /**
     * Create a new VAO with this VBO, EBO, and vertex format attached, but not yet bound in GL server RAM.
     * @param vbo The vertex buffer for this array object.
     * @param ebo The element buffer for this array object.
     * @param format The vertex format for this array object.
     */
    public VAO(VBO vbo, EBO ebo, VertexFormat format) {
        vao = GL32.glGenVertexArrays();
        this.format = format;
        this.vbo = vbo;
        this.ebo = ebo;
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
        //TODO save current state, there will be more buffers to restore when we actually implement them
        //save current state
        /*bound buffers are global and not tracked by the VAO, the VAO only stores the data from them needed for rendering
        binding buffers other than the VAO is mainly just for writing pointer references to those buffers to the VAO
        binding a different VAO will allow you to render the data attached to that VAO previously but won't rebind
        the buffers that were bound when that VAO was last bound*/
        int oldVao = GL32.glGetInteger(GL32.GL_VERTEX_ARRAY_BUFFER_BINDING);
        int oldVbo = GL32.glGetInteger(GL32.GL_ARRAY_BUFFER);
        //EBO binding is attached to VAO, when you change the bound VAO it changes the EBO for you
        GL32.glBindVertexArray(vao);
        if(vbo != null){
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.getVbo());
            vbo.init();//TODO investigate threading
            vbo.freeSystemRAM();//TODO investigate system RAM management
        } else {
            ModCore.warn("tried to init a VAO with no VBO, don't do this!");
        }
        if(ebo != null) {//TODO implement EBO
            GL32.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo.getEbo());
        }
        if(format != null) {
            format.apply();
        } else {
            ModCore.warn("VAO inited without vertex format, defaulting to BLIT_SCREEN");
            VertexFormats.BLIT_SCREEN.apply();
        }
        inited = true;
        //restore previous state
        GL32.glBindVertexArray(oldVao);
        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, oldVbo);
    }

    /**
     * Binds a set of VAOs sequentially to avoid redundant state restores.
     * @param vaos list of VAOs to be inited.
     */
    public static void init(List<VAO> vaos) {
        //save current state
        int oldVao = GL32.glGetInteger(GL32.GL_VERTEX_ARRAY_BUFFER_BINDING);
        int oldVbo = GL32.glGetInteger(GL32.GL_ARRAY_BUFFER);
        //init
        for(VAO array : vaos) {
            if(array.inited) continue;
            GL32.glBindVertexArray(array.vao);
            if(array.vbo != null) {
                GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, array.vbo.getVbo());
                array.vbo.init();
                //array.vbo.freeRAM();
            } else {
                ModCore.warn("tried to init a VAO with no VBO, don't do this!");
                break;
            }
            if(array.ebo != null) {

            }
            if(array.format != null) {
                array.format.apply();
            } else {
                //default format? maybe this one?
                ModCore.warn("VAO inited without vertex format, defaulting to BLIT_SCREEN");
                VertexFormats.BLIT_SCREEN.apply();
            }
            array.inited = true;
        }
        //restore previous state
        GL32.glBindVertexArray(oldVao);
        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, oldVbo);
    }

    /**
     * Function to apply a vertex format to this vao.<br>
     * Only call while this vao is bound
     * @param format vertex format to be applied
     */
    private void applyFormat(VertexFormat format) {
        //depricated?
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
