package cam72cam.mod.automate;

import cam72cam.mod.util.CollectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuiClickSlot extends Action {
    public static final String TYPE = "GuiClickSlot";
    private String slotId;
    private ClickType clickType;

    enum ClickType {
        Left(0),
        Right(1),
        Middle(2);

        private final int id;

        ClickType(int i) {
            this.id = i;
        }
    }

    public GuiClickSlot(String... params) {
        super(TYPE);
        slotId = params[0];
        clickType = ClickType.valueOf(params[1]);
    }

    @Override
    public List<String> getParams() {
        return CollectionUtil.listOf(slotId, clickType.name());
    }

    @Override
    public boolean tick() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof GuiContainer) {
            GuiContainer container = (GuiContainer) screen;
            try {
                Method mouseClicked = GuiContainer.class.getDeclaredMethod("mouseClicked", int.class, int.class, int.class);
                Method mouseReleased = GuiContainer.class.getDeclaredMethod("mouseReleased", int.class, int.class, int.class);
                mouseClicked.setAccessible(true);
                mouseReleased.setAccessible(true);
                int slotId = Integer.parseInt(this.slotId);
                int max = container.inventorySlots.inventorySlots.size();
                if (slotId < max) {
                    Slot slot = container.inventorySlots.inventorySlots.get(slotId);
                    mouseClicked.invoke(container, slot.xPos + container.getGuiLeft(), slot.yPos + container.getGuiTop(), clickType.id);
                    mouseReleased.invoke(container, slot.xPos + container.getGuiLeft(), slot.yPos + container.getGuiTop(), clickType.id);
                    return true;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            //container.mc.playerController.windowClick(container.inventorySlots.windowId, Integer.parseInt(slotId), clickType.id, net.minecraft.inventory.ClickType.SWAP, container.mc.player);
        }
        return false;
    }

    @Override
    public void renderEditor(JComponent panel) {
        String stackInfo = getStack() != null ? " (has " + getStack().getDisplayName() + ")" : "";
        JComboBox<ClickType> ct = new JComboBox<>(ClickType.values());
        ct.setSelectedItem(clickType);
        ct.addItemListener(a -> clickType = (ClickType)ct.getSelectedItem());
        ct.setVisible(true);
        panel.add(ct);

        JLabel l = new JLabel(" Click Slot ");
        l.setVisible(true);
        panel.add(l);

        JLabel l2 = new JLabel(stackInfo);
        l2.setVisible(true);

        JTextField tn = new JTextField(slotId);
        tn.getDocument().addDocumentListener((TextListener)() -> {
            slotId = tn.getText();
            String stackInfo2 = getStack() != null ? " (has " + getStack().getDisplayName() + ")" : "";
            l2.setText(stackInfo2);
        });
        tn.setVisible(true);
        panel.add(tn);

        panel.add(l2);
    }

    @Override
    public void renderSummary(JComponent panel) {
        String stackInfo = getStack() != null ? " (has " + getStack().getDisplayName() + ")" : "";
        JLabel l = new JLabel(clickType.toString() + " Click Slot " + slotId + stackInfo);
        l.setVisible(true);
        panel.add(l);
    }

    private ItemStack getStack() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof GuiContainer) {
            GuiContainer container = (GuiContainer) screen;
            int slotId = Integer.parseInt(this.slotId);
            int max = container.inventorySlots.inventorySlots.size();
            if (slotId < max) {
                Slot slot = container.inventorySlots.inventorySlots.get(slotId);
                return slot.getStack();
            }
        }
        return null;
    }

    public static List<Action> getPotential() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof GuiContainer) {
            GuiContainer container = (GuiContainer) screen;
            return container.inventorySlots.inventorySlots.stream().map(
                    slot -> new GuiClickSlot(slot.slotNumber + "", "Left")
            ).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

}
