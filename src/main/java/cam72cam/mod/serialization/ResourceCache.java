package cam72cam.mod.serialization;

import cam72cam.mod.ModCore;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.ThrowingFunction;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
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

        private final Map<Identifier, Pair<HashCode, byte[]>> resources = new HashMap<>();
        private final Set<Identifier> accessed = new HashSet<>();

        private static ResourceProvider fromTag(TagCompound data) {
            ResourceProvider provider = new ResourceProvider();
            Map<Identifier, String> expected = data.getMap("resources", Identifier::new, v -> v.getString("key"));
            try {
                for (Identifier id : expected.keySet()) {
                    String expectedHash = expected.get(id);
                    String foundHash = hashCache.containsKey(id) ? hashCache.get(id) : provider.get(id).getKey().toString();
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
                expected.put(identifier, resources.get(identifier).getKey().toString());
            }
            return new TagCompound().setMap("resources", expected, Identifier::toString, v -> new TagCompound().setString("key", v));
        }

        private Pair<HashCode, byte[]> get(Identifier id) {
            if (!resources.containsKey(id)) {
                try (
                        HashingInputStream source = new HashingInputStream(ResourceCache.hasher, id.getLastResourceStream());
                        ByteArrayOutputStream sink = new ByteArrayOutputStream(1024 * 1024)
                ) {
                    IOUtils.copy(source, sink);
                    HashCode hash = source.hash();
                    resources.put(id, Pair.of(hash, sink.toByteArray()));
                    hashCache.put(id, hash.toString());
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
    private boolean isClosed;
    private ThrowingFunction<ResourceProvider, T, IOException> constructor;

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

    interface IOExec {
        GenericByteBuffer exec() throws IOException;
    }

    public Supplier<GenericByteBuffer> getResource(String name, Function<T, GenericByteBuffer> converter) throws IOException {
        File file = new File(dir, name + ".lz4");
        IOExec genData = () -> {
            NullPointerException ex = null;
            GenericByteBuffer in = null;
            for (int retry = 0; retry < 10; retry++) {
                try (FileChannel channel = new FileOutputStream(file).getChannel()) {
                    in = converter.apply(intermediary);
                    LZ4Factory factory = LZ4Factory.fastestInstance();
                    LZ4Compressor compressor = factory.highCompressor(2);
                    // Could be faster
                    byte[] output = compressor.compress(in.buffer.array());
                    ByteBuffer prefix = ByteBuffer.wrap(new byte[Integer.BYTES]);
                    prefix.asIntBuffer().put(in.buffer.capacity());
                    channel.write(prefix);
                    channel.write(ByteBuffer.wrap(output));
                    channel.force(true);
                } catch (NullPointerException e) {
                    ModCore.error("Hit an exception while compressing cache data!  If you are using Java OpenJ9, please use a different JVM as there are known memory corruption bugs.");
                    ex = e;
                }
            }
            if (ex != null) {
                throw ex;
            }
            return in;
        };

        if (intermediary != null) {
            genData.exec();
        }
        return () -> {
            try (FileChannel channel = new FileInputStream(file).getChannel()) {
                MappedByteBuffer raw = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                GenericByteBuffer out = new GenericByteBuffer(new byte[raw.getInt()]);
                LZ4Factory factory = LZ4Factory.fastestInstance();
                LZ4FastDecompressor decompressor = factory.fastDecompressor();
                decompressor.decompress(raw, out.buffer);
                out.buffer.position(0);
                return out;
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    // Terrible Haxxx
                    provider = new ResourceProvider();
                    intermediary = constructor.apply(provider);
                    return genData.exec();
                } catch (IOException ex) {
                    throw new RuntimeException();
                }
            }
        };
    }

    public String close() throws IOException {
        if (provider != null) {
            Files.write(meta.toPath(), provider.toTag().toBytes());
        }
        isClosed = true;
        return hasher.hashBytes(Files.readAllBytes(meta.toPath())).toString();
    }

    public static class GenericByteBuffer {
        private final ByteBuffer buffer;

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
