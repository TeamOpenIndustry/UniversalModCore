package cam72cam.mod.event.platform;

import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.List;
import java.util.Optional;

/**
 * Our own event to stitch sprite to InventoryMenu.BLOCK_ATLAS
 * <p>
 * This event is in replacement of net.minecraftforge.client.event.TextureStitchEvent.Pre which was removed
 */
public class TextureStitchEvent extends Event implements IModBusEvent {
	private final List<SpriteSource> spriteSources;

	public TextureStitchEvent(List<SpriteSource> spriteSources) {
		this.spriteSources = spriteSources;
	}

	public void addSprite(ResourceLocation location) {
		this.addSprite(new SingleFile(location, Optional.empty()));
	}

	public void addSprite(SpriteSource source) {
		this.spriteSources.add(source);
	}
}
