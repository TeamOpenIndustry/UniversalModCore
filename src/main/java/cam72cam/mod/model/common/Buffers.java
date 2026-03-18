package cam72cam.mod.model.common;

public class Buffers {
    public static class FloatBuffer {
        private int pos;
        private float[] buffer;
        public FloatBuffer(int startingSize) {
            pos = 0;
            buffer = new float[startingSize];
        }

        public FloatBuffer add(float f) {
            if (pos == buffer.length) {
                float[] newBuffer = new float[buffer.length * 2];
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                buffer = newBuffer;
            }
            buffer[pos] = f;
            pos++;
            return this;
        }

        public FloatBuffer add(float a, float b) {
            return this.add(a).add(b);
        }

        public FloatBuffer add(float a, float b, float c) {
            return this.add(a).add(b).add(c);
        }

        public FloatBuffer add(float a, float b, float c, float d) {
            return this.add(a).add(b).add(c).add(d);
        }

        public float[] array() {
            float[] newBuffer = new float[pos];
            System.arraycopy(buffer, 0, newBuffer, 0, pos);
            return newBuffer;
        }

        public float[] currentArray() {
            return buffer;
        }

        public int size() {
            return pos;
        }
    }

    public static class IntBuffer {
        private int pos;
        private int[] buffer;
        public IntBuffer(int startingSize) {
            pos = 0;
            buffer = new int[startingSize];
        }

        public IntBuffer add(int f) {
            if (pos == buffer.length) {
                int[] newBuffer = new int[buffer.length * 2];
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                buffer = newBuffer;
            }
            buffer[pos] = f;
            pos++;
            return this;
        }

        public IntBuffer add(int a, int b) {
            return this.add(a).add(b);
        }

        public IntBuffer add(int a, int b, int c) {
            return this.add(a).add(b).add(c);
        }

        public int[] array() {
            int[] newBuffer = new int[pos];
            System.arraycopy(buffer, 0, newBuffer, 0, pos);
            return newBuffer;
        }

        public int size() {
            return pos;
        }
    }
}
