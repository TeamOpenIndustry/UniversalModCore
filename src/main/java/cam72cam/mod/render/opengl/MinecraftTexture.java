package cam72cam.mod.render.opengl;

import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.SimpleResource;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MinecraftTexture implements Texture {
    private final Identifier id;

    public MinecraftTexture(Identifier id) {
        this.id = id;
    }

    @Override
    public int getId() {
        TextureManager texManager = Minecraft.getInstance().getTextureManager();

        net.minecraft.client.renderer.texture.Texture tex = texManager.getTexture(id.internal);
        //noinspection ConstantConditions
        if (tex == null) {
            try {
                IResource resource = Minecraft.getInstance().getResourceManager().getResource(id.internal);
                texManager.register(id.internal, new SimpleTexture(id.internal));
                tex = texManager.getTexture(id.internal);
            } catch (Exception ex) {
                // Pass
            }

            if (tex == null) {
                // Fallback to zips
                texManager.register(id.internal, new SimpleTexture(id.internal) {
                    @Override
                    protected SimpleTexture.TextureData getTextureImage(IResourceManager p_215246_1_) {
                        return SimpleTexture.TextureData.load(new IResourceManager() {
                            @Override
                            public Set<String> getNamespaces() {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public IResource getResource(ResourceLocation resourceLocationIn) throws IOException {
                                return new SimpleResource("internal", MinecraftTexture.this.id.internal, MinecraftTexture.this.id.getResourceStream(), null);
                            }

                            @Override
                            public boolean hasResource(ResourceLocation p_219533_1_) {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public List<IResource> getResources(ResourceLocation p_199004_1_) throws IOException {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public Collection<ResourceLocation> listResources(String p_199003_1_, Predicate<String> p_199003_2_) {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public Stream<IResourcePack> listPacks() {
                                throw new RuntimeException("INVALID");
                            }
                        }, this.location);
                    }
                });
                tex = texManager.getTexture(id.internal);
            }
        }
        return tex.getId();
    }
}
