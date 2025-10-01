package rendertrack.vertex;

import java.io.IOException;

/**
 *
 */
public class VBO {
    private int vbo;
    private float[] data;
    private boolean loaded;
    private boolean buffered;

    /**
     *
     * @return the int reference to the VBO.
     */
    public int getVbo() {
        return vbo;
    }

    /**
     * Bind this buffer into GL server RAM if needed.
     */
    protected void init() {//TODO should probably throw an exception if no vertex data has been loaded
        if(buffered) return;
        //otherwise we need to write it to the buffer and wait for that to finish
    }

    /**
     * Write the vertex data to a GL server RAM buffer.<br>
     * Separated from VAO binding for performance considerations.
     * Performing this write at least one frame before you need the model will help prevent stuttering.
     */
    public void writeBuffer() {
        //TODO
        buffered = true;
    }
    //TODO can we have a readBuffer?

    //TODO investigate disk cache requirements
    /**
     * Perform an async threaded read from disk into client RAM, does not write the GL buffer.
     * @throws InterruptedException
     * @throws IOException
     */
    public void readFromDisk() throws InterruptedException, IOException {
        //TODO
    }

    //TODO these two probably need some form of access from external mods so they can manage their own performance considerations. But probably don't expose these methods, too low level, too much could go wrong.
    /**
     * Call after binding buffer into GL server RAM to remove it from client RAM.
     */
    protected void freeSystemRAM() {
        data = null;
        loaded = false;
    }

    /**
     * Call to free this buffer in GL server RAM.
     */
    protected void freeGLRAM() {
        buffered = false;
    }
}
