package cam72cam.mod.model.common.util;

public class Buffers {
    public static class FloatBuffer {
        private int pos;
        private float[] buffer;
        public FloatBuffer(int startingSize) {
            pos = 0;
            buffer = new float[startingSize];
        }

        public FloatBuffer(float[] data) {
            pos = data.length;
            buffer = new float[data.length];
            System.arraycopy(data, 0, buffer, 0, data.length);
        }

        public static FloatBuffer wrap(float[] original) {
            FloatBuffer buffer = new FloatBuffer(0);
            buffer.pos = original.length;
            buffer.buffer = original;
            return buffer;
        }

        public void add(float f) {
            if (pos == buffer.length) {
                float[] newBuffer = new float[buffer.length * 2];
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                buffer = newBuffer;
            }
            buffer[pos] = f;
            pos++;
        }

        //For writable implementation
        public float[] array() {
            float[] newBuffer = new float[pos];
            System.arraycopy(buffer, 0, newBuffer, 0, pos);
            return newBuffer;
        }

        //For read-only implementation
        public float[] raw() {
            return buffer;
        }

        public float get(int index) {
            if (index >= pos) {
                throw new IndexOutOfBoundsException();
            }
            return buffer[index];
        }

        public int size() {
            return pos;
        }

        public void clear() {
            pos = 0;
        }
    }

    public static class IntBuffer {
        private int pos;
        private int[] buffer;
        public IntBuffer(int startingSize) {
            pos = 0;
            buffer = new int[startingSize];
        }

        public IntBuffer(int[] original) {
            pos = original.length;
            buffer = new int[original.length];
            System.arraycopy(original, 0, buffer, 0, original.length);
        }

        public static IntBuffer wrap(int[] original) {
            IntBuffer buffer = new IntBuffer(0);
            buffer.pos = original.length;
            buffer.buffer = original;
            return buffer;
        }

        public void add(int f) {
            if (pos == buffer.length) {
                int[] newBuffer = new int[buffer.length * 2];
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                buffer = newBuffer;
            }
            buffer[pos] = f;
            pos++;
        }

        //For writable implementation
        public int[] array() {
            int[] newBuffer = new int[pos];
            System.arraycopy(buffer, 0, newBuffer, 0, pos);
            return newBuffer;
        }

        //For read-only implementation
        public int[] raw() {
            return buffer;
        }

        public int get(int index) {
            if (index >= pos) {
                throw new IndexOutOfBoundsException();
            }
            return buffer[index];
        }

        public int size() {
            return pos;
        }

        public void clear() {
            pos = 0;
        }
    }
}
