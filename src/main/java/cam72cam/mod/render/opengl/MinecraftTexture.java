package cam72cam.mod.render.opengl;

import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
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

public class MinecraftTexture implements Texture {
    private final Identifier id;

    public MinecraftTexture(Identifier id) {
        this.id = id;
    }

    @Override
    public int getId() {
        TextureManager texManager = Minecraft.getInstance().getTextureManager();

        ITextureObject tex = texManager.getTexture(id.internal);
        //noinspection ConstantConditions
        if (tex == null) {
            try {
                IResource resource = Minecraft.getInstance().getResourceManager().getResource(id.internal);
                texManager.loadTexture(id.internal, new SimpleTexture(id.internal));
                tex = texManager.getTexture(id.internal);
            } catch (Exception ex) {
                // Pass
            }

            if (tex == null) {
                // Fallback to zips
                texManager.loadTexture(id.internal, new SimpleTexture(id.internal) {
                    @Override
                    protected TextureData func_215246_b(IResourceManager p_215246_1_) {
                        return SimpleTexture.TextureData.func_217799_a(new IResourceManager() {
                            @Override
                            public Set<String> getResourceNamespaces() {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public IResource getResource(ResourceLocation resourceLocationIn) throws IOException {
                                return new SimpleResource("internal", id.internal, id.getResourceStream(), null);
                            }

                            @Override
                            public boolean hasResource(ResourceLocation p_219533_1_) {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public List<IResource> getAllResources(ResourceLocation resourceLocationIn) throws IOException {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public Collection<ResourceLocation> getAllResourceLocations(String pathIn, Predicate<String> filter) {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public void addResourcePack(IResourcePack resourcePack) {
                                throw new RuntimeException("INVALID");
                            }
                        }, this.textureLocation);
                    }
                });
                tex = texManager.getTexture(id.internal);
            }
        }
        return tex.getGlTextureId();
    }
}
