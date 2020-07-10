package cam72cam.mod.automate;

public class Automation {
    public static final Automation INSTANCE = new Automation();
    public UserInterface UI;
    public void tick() {
        if (UI == null) {
            UI = new UserInterface();
        }
        UI.tick();
    }

    static {
        Action.register(GuiClickButton.TYPE, GuiClickButton::new, GuiClickButton::getPotential);
        Action.register(GuiSetText.TYPE, GuiSetText::new, GuiSetText::getPotential);
        Action.register(KeyPress.TYPE, KeyPress::new, KeyPress::getPotential);
        Action.register(PlayerLook.TYPE, PlayerLook::new, PlayerLook::getPotential);
        Action.register(GuiSelectWorld.TYPE, GuiSelectWorld::new, GuiSelectWorld::getPotential);
        Action.register(WaitTicks.TYPE, WaitTicks::new, WaitTicks::getPotential);
    }

}
