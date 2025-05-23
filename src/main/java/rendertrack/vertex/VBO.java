package rendertrack.vertex;
/**
 *
 */
public class VBO {
    private int vbo;

    /**
     *
     * @return the int reference to the VBO.
     */
    public int getVbo() {
        return vbo;
    }

    /**
     * Bind this buffer to the current VAO.
     */
    protected void init() {

    }

    /**
     * Call after binding buffer into VRAM to remove it from system RAM.
     */
    protected void freeRAM() {

    }
}
