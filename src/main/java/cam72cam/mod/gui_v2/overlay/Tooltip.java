package cam72cam.mod.gui_v2.overlay;

import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Tooltip extends PostEffect {
    private List<String> messages;

    public void setMessages(@Nullable List<PlayerMessage> messages) {
        if(messages == null) {
            this.messages = null;
        } else {
            this.messages = messages.stream().map(m -> m.internal.getFormattedText()).collect(Collectors.toList());
        }
    }

    public void setMessage(@Nullable PlayerMessage message) {
        if(message == null) {
            this.messages = null;
        } else {
            this.messages = Collections.singletonList(message.internal.getFormattedText());
        }
    }

    @Override
    public void drawAt(GuiRenderer guiRenderer, int x, int y) {
        if (messages != null && !messages.isEmpty()) {
            net.minecraftforge.fml.client.config.GuiUtils
                    .drawHoveringText(this.messages, x, y, GuiUtils.getScreenWidth(), GuiUtils.getScreenHeight(), -1, Minecraft.getMinecraft().fontRenderer);
        }
    }

    @Override
    public boolean isAlive() {
        return true;
    }
}
