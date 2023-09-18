package cam72cam.mod.render.opengl;

import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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

        AbstractTexture tex = texManager.getTexture(id.internal);
        //noinspection ConstantConditions
        if (tex == null) {
            try {
                Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id.internal);
                if (resource.isPresent()) {
                    texManager.register(id.internal, new SimpleTexture(id.internal));
                    tex = texManager.getTexture(id.internal);
                }
            } catch (Exception ex) {
                // Pass
            }

            if (tex == null) {
                // Fallback to zips
                texManager.register(id.internal, new SimpleTexture(id.internal) {
                    @Override
                    protected SimpleTexture.TextureImage getTextureImage(ResourceManager p_118140_) {
                        return SimpleTexture.TextureImage.load(new ResourceManager() {
                            @Override
                            public Set<String> getNamespaces() {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public List<Resource> getResourceStack(ResourceLocation p_10730_) {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public Map<ResourceLocation, Resource> listResources(String p_215563_, Predicate<ResourceLocation> p_215564_) {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public Map<ResourceLocation, List<Resource>> listResourceStacks(String p_215565_, Predicate<ResourceLocation> p_215566_) {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public Stream<PackResources> listPacks() {
                                throw new RuntimeException("INVALID");
                            }

                            @Override
                            public Optional<Resource> getResource(ResourceLocation resourceLocationIn) {
                                return Optional.of(new Resource(null, MinecraftTexture.this.id::getResourceStream));
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
