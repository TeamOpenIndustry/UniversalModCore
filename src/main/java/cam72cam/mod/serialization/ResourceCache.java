package cam72cam.mod.serialization;

import cam72cam.mod.ModCore;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.resource.Identifier.InputStreamMod;
import cam72cam.mod.util.ThrowingFunction;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class ResourceCache<T> {
    private static final HashFunction hasher = Hashing.murmur3_128();

    public static class ResourceProvider implements Function<Identifier, byte[]> {
        // TODO This might need to be cleared when MC packs are reloaded...
        private final static Map<Identifier, String> hashCache = new HashMap<>();

        private final Map<Identifier, Pair<String, byte[]>> resources = new HashMap<>();
        private final Set<Identifier> accessed = new HashSet<>();

        private static ResourceProvider fromTag(TagCompound data) {
            ResourceProvider provider = new ResourceProvider();
            Map<Identifier, String> expected = data.getMap("resources", Identifier::new, v -> v.getString("key"));
            try {
                for (Identifier id : expected.keySet()) {
                    String expectedHash = expected.get(id);
                    String foundHash;
                    synchronized (hashCache) {
                        foundHash = hashCache.get(id);
                    }
                    if (foundHash == null) {
                        try (InputStream stream = id.getLastResourceStream()) {
                            if (stream instanceof InputStreamMod) {
                                foundHash = "MOD" + ((InputStreamMod) stream).time;
                            } else {
                                foundHash = provider.get(id).getKey();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (!expectedHash.equals(foundHash)) {
                        return provider;
                    }
                }
            } catch (RuntimeException ex) {
                ModCore.catching(ex);
                return null;
            }
            return null;
        }

        private TagCompound toTag() {
            Map<Identifier, String> expected = new HashMap<>();
            for (Identifier identifier : accessed) {
                expected.put(identifier, resources.get(identifier).getKey());
            }
            return new TagCompound().setMap("resources", expected, Identifier::toString, v -> new TagCompound().setString("key", v));
        }

        private Pair<String, byte[]> get(Identifier id) {
            if (!resources.containsKey(id)) {
                try (
                        InputStream stream = id.getLastResourceStream();
                        InputStreamMod ism = stream instanceof InputStreamMod ? (InputStreamMod)stream : null;
                        HashingInputStream hashing =  ism == null ? new HashingInputStream(ResourceCache.hasher, stream) : null;
                        InputStream source = hashing != null ? hashing : ism;
                        ByteArrayOutputStream sink = new ByteArrayOutputStream(1024 * 1024)
                ) {
                    IOUtils.copy(source, sink);
                    String hash = hashing != null ? hashing.hash().toString() : "MOD" + ism.time;
                    resources.put(id, Pair.of(hash, sink.toByteArray()));
                    synchronized (hashCache) {
                        hashCache.put(id, hash);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return resources.get(id);
        }

        @Override
        public byte[] apply(Identifier id) {
            byte[] value = get(id).getValue();
            accessed.add(id);
            return value;
        }
    }

    private final File dir;
    private final File meta;
    private ResourceProvider provider;
    private T intermediary;
    private final ThrowingFunction<ResourceProvider, T, IOException> constructor;

    public ResourceCache(Identifier id, ThrowingFunction<ResourceProvider, T, IOException> constructor) throws IOException {
        dir = ModCore.cacheFile(id);
        dir.mkdirs();
        meta = new File(dir, "meta.nbt");
        this.constructor = constructor;
        provider = meta.exists() ?
                ResourceProvider.fromTag(new TagCompound(Files.readAllBytes(meta.toPath()))) :
                new ResourceProvider();
        intermediary = provider != null ? constructor.apply(provider) : null;
    }

    private static void writeBuffer(File file, ByteBuffer buffer) throws IOException {
        buffer.position(0);
        try (FileChannel channel = new FileOutputStream(file).getChannel()) {
            LZ4Factory factory = LZ4Factory.fastestInstance();
            LZ4Compressor compressor = factory.highCompressor(2);

            // Could be faster
            byte[] input = buffer.array();
            byte[] output = compressor.compress(input);

            // Write number of input bytes
            ByteBuffer prefix = ByteBuffer.allocate(Integer.BYTES);
            prefix.asIntBuffer().put(input.length);
            channel.write(prefix);

            // Write the compressed data
            channel.write(ByteBuffer.wrap(output));
        } catch (NullPointerException e) {
            ModCore.error("Hit an exception while compressing cache data!  If you are using Java OpenJ9, please use a different JVM as there are known memory corruption bugs.");
            throw e;
        }
    }
    private static ByteBuffer readBuffer(File file) throws IOException {
        try (FileChannel channel = new FileInputStream(file).getChannel()) {
            LZ4Factory factory = LZ4Factory.fastestInstance();
            LZ4FastDecompressor decompressor = factory.fastDecompressor();

            // Memmap the file (per javadoc, hooks into GC cleanup)
            MappedByteBuffer raw = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

            // Read number of input bytes
            int decompressedBytes = raw.getInt();
            // Buffer to hold the decompressed data
            ByteBuffer decompressed = ByteBuffer.allocate(decompressedBytes);

            // Perform the decompression and move the head to the beginning of the buffer
            decompressor.decompress(raw, decompressed);
            decompressed.position(0);

            return decompressed;
        }
    }

    private GenericByteBuffer regenerateBuffer(File file, Function<T, GenericByteBuffer> converter) throws IOException {
        GenericByteBuffer gen = converter.apply(constructor.apply(new ResourceProvider()));
        writeBuffer(file, gen.buffer);
        gen.buffer.position(0);
        return gen;
    }

    public Supplier<GenericByteBuffer> getResource(String name, Function<T, GenericByteBuffer> converter) throws IOException {
        File file = new File(dir, name + ".lz4");

        if (intermediary != null) {
            writeBuffer(file, converter.apply(intermediary).buffer);
        } else if (!file.exists() || file.length() < Integer.BYTES) {
            // This sometimes happens on windows or after a failed launch attempt.
            regenerateBuffer(file, converter);
        }

        return () -> {
            if (!file.exists() || file.length() < Integer.BYTES) {
                // This sometimes happens on windows or after a failed launch attempt.
                try {
                    return regenerateBuffer(file, converter);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                return new GenericByteBuffer(readBuffer(file));
            } catch (IOException e) {
                // Hail Mary!
                try {
                    return regenerateBuffer(file, converter);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public String close() throws IOException {
        if (provider != null) {
            Files.write(meta.toPath(), provider.toTag().toBytes());
        }
        provider = null;
        intermediary = null;
        return hasher.hashBytes(Files.readAllBytes(meta.toPath())).toString();
    }

    public static class GenericByteBuffer {
        private final ByteBuffer buffer;

        public GenericByteBuffer(ByteBuffer buffer) {
            this.buffer = buffer;
        }
        public GenericByteBuffer(byte[] bytes) {
            this.buffer = ByteBuffer.wrap(bytes);
        }
        public GenericByteBuffer(int[] ints) {
            this.buffer = ByteBuffer.allocate(ints.length * Integer.BYTES);
            this.buffer.asIntBuffer().put(ints);
        }
        public GenericByteBuffer(float[] floats) {
            this.buffer = ByteBuffer.allocate(floats.length * Float.BYTES);
            this.buffer.asFloatBuffer().put(floats);
        }

        public byte[] bytes() {
            return buffer.array();
        }
        public int[] ints() {
            int[] ints = new int[buffer.capacity() / Integer.BYTES];
            buffer.asIntBuffer().get(ints);
            return ints;
        }
        public float[] floats() {
            float[] floats = new float[buffer.capacity() / Float.BYTES];
            buffer.asFloatBuffer().get(floats);
            return floats;
        }
    }
}
