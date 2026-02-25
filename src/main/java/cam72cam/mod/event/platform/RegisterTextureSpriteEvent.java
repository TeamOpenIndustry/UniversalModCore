package cam72cam.mod.event.platform;

import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.List;
import java.util.Optional;

/**
 * Fired when texture stitch datapacks are reloaded
 */
public class RegisterTextureSpriteEvent extends Event implements IModBusEvent {
	private final List<SpriteSource> spriteSources;

	public RegisterTextureSpriteEvent(List<SpriteSource> spriteSources) {
		this.spriteSources = spriteSources;
	}

	/**
	 * Add custom sprite to <code>InventoryMenu.BLOCK_ATLAS</code>
	 * @param location Your custom texture
	 */
	public void addSprite(ResourceLocation location) {
		this.addSprite(new SingleFile(location, Optional.empty()));
	}

	public void addSprite(SpriteSource source) {
		this.spriteSources.add(source);
	}
}
