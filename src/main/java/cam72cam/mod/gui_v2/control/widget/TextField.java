package cam72cam.mod.gui_v2.control.widget;

import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.gui_v2.core.actions.IFocusable;
import cam72cam.mod.gui_v2.core.actions.IKeyboardListener;
import cam72cam.mod.gui_v2.core.actions.IUpdatable;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.input.Clipboard;
import cam72cam.mod.input.Keyboard;
import com.google.common.base.Predicates;

import java.util.function.Consumer;
import java.util.function.Predicate;

//TODO Multiline support
public class TextField extends AbstractWidget<TextField>
        implements IClickable, IFocusable, IKeyboardListener, IUpdatable {
    private static final int SPAN = GuiRenderer.TEXT_HEIGHT / 2;

    private String text;
    private Consumer<String> callback;
    private Predicate<String> validator = Predicates.alwaysTrue();
    private int cursorPos = 0;
    private int textOffsetX = 0;
    private int selectionStart = -1;

    private int cursorBlinkTimer = 0;

    private boolean focusing;

    public TextField(int width, int height, Consumer<String> callback) {
        this(width, height, "", callback);
    }

    public TextField(int width, int height, String original, Consumer<String> callback) {
        this.setWidth(width);
        this.setHeight(height);
        this.text = original;
        this.callback = callback;

        this.setVanillaFacade();
    }

    public void setValidator(Predicate<String> validator) {
        this.validator = validator;
    }

    @Override
    public void layout(int x, int y) {
        setX(x);
        setY(y);
    }

    @Override
    public boolean onKeyPressed(Keyboard.KeyCode key) {
        if (!focusing) return false;

        switch (key) {
            case LEFT:
                if (Keyboard.isPressingShift()) {
                    if (selectionStart < 0) selectionStart = cursorPos;
                } else {
                    clearSelection();
                }
                setCursorPos(cursorPos - 1);
                return true;
            case RIGHT:
                if (Keyboard.isPressingShift()) {
                    if (selectionStart < 0) selectionStart = cursorPos;
                } else {
                    clearSelection();
                }
                setCursorPos(cursorPos + 1);
                return true;
            case HOME:
                if (Keyboard.isPressingShift()) {
                    if (selectionStart < 0) selectionStart = cursorPos;
                } else {
                    clearSelection();
                }
                setCursorPos(0);
                return true;
            case END:
                if (Keyboard.isPressingShift()) {
                    if (selectionStart < 0) selectionStart = cursorPos;
                } else {
                    clearSelection();
                }
                setCursorPos(text.length());
                return true;
            case BACK:
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPos > 0) {
                    String newText = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
                    if (canApplyText(newText)) {
                        setCursorPos(cursorPos - 1);
                    }
                }
                return true;
            case DELETE:
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPos < text.length()) {
                    String newText = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
                    canApplyText(newText);
                    setCursorPos(cursorPos); //Trigger updates
                }
                return true;
            case A:
                if (Keyboard.isPressingCtrl()) {
                    selectionStart = 0;
                    setCursorPos(text.length());
                    return true;
                }
                break;
            case C:
                if (Keyboard.isPressingCtrl()) {
                    if (hasSelection()) {
                        Clipboard.setClipboard(getSelectedText());
                    }
                    return true;
                }
                break;
            case X:
                if (Keyboard.isPressingCtrl()) {
                    if (hasSelection()) {
                        Clipboard.setClipboard(getSelectedText());
                        deleteSelection();
                    }
                    return true;
                }
                break;
            case V:
                if (Keyboard.isPressingCtrl()) {
                    String clipboard = Clipboard.getClipboard();
                    if (!clipboard.isEmpty()) {
                        insertText(clipboard);
                    }
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onCharTyped(char ch) {
        if (!focusing) return false;
        insertChar(ch);
        return true;
    }

    @Override
    public void onTick() {
        cursorBlinkTimer ++;
    }

    public void setCursorPos(int newPos) {
        cursorPos = MathUtil.clamp(newPos, 0, text.length());
        cursorBlinkTimer = 0; //Reset blinker

        //Refine text offset to display in bounds
        int visibleWidth = this.width() - 2 * SPAN;
        int totalTextWidth = GuiUtils.getTextWidth(text);
        int cursorPixelX = GuiUtils.getTextWidth(text.substring(0, cursorPos));

        int newOffset = this.textOffsetX;
        if (totalTextWidth > visibleWidth) {
            if (cursorPixelX < newOffset) {
                newOffset = cursorPixelX;
            } else if (cursorPixelX > newOffset + visibleWidth) {
                newOffset = cursorPixelX - visibleWidth;
            }
            newOffset = Math.min(newOffset, totalTextWidth - visibleWidth);
            newOffset = Math.max(newOffset, 0);
        } else {
            newOffset = 0;
        }
        this.textOffsetX = newOffset;
    }

    public void clearSelection() {
        selectionStart = -1;
    }

    public boolean hasSelection() {
        return selectionStart >= 0 && selectionStart != cursorPos;
    }

    private boolean canApplyText(String newText) {
        if (!validator.test(newText)) {
            return false;
        }
        this.text = newText;
        setCursorPos(cursorPos); //Trigger updates
        if (callback != null) {
            callback.accept(text);
        }
        return true;
    }

    private String getSelectedText() {
        if (!hasSelection()) return "";
        int start = Math.min(selectionStart, cursorPos);
        int end = Math.max(selectionStart, cursorPos);
        return text.substring(start, end);
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        int start = Math.min(selectionStart, cursorPos);
        int end = Math.max(selectionStart, cursorPos);
        String newText = text.substring(0, start) + text.substring(end);
        if (canApplyText(newText)) {
            setCursorPos(start);
            clearSelection();
        }
    }

    private void insertChar(char c) {
        insertText(String.valueOf(c));
    }

    private void insertText(String str) {
        deleteSelection();
        String newText = text.substring(0, cursorPos) + str + text.substring(cursorPos);
        if (canApplyText(newText)) {
            setCursorPos(cursorPos + str.length());
        }
    }

    @Override
    public void onFocusGained() {
        focusing = true;
        cursorBlinkTimer = 0;
    }

    @Override
    public void onFocusLost() {
        focusing = false;
    }

    @Override
    public boolean onClick(Player.Hand hand, int mouseX, int mouseY) {
        if (!isHovering()) {
            return false;
        }

        if(!focusing) {
            requestFocus(this);
        }

        int relativeX = mouseX - x() + textOffsetX;
        int bestPos = 0;
        int bestDist = Integer.MAX_VALUE;
        int xOff = SPAN;
        int currX = xOff;
        for (int i = 0; i <= text.length(); i++) {
            int dist = Math.abs(relativeX - currX);
            if (dist < bestDist) {
                bestDist = dist;
                bestPos = i;
            }
            if (i < text.length()) {
                currX += GuiUtils.getTextWidth(String.valueOf(text.charAt(i)));
            }
        }

        if (Keyboard.isPressingShift()) {
            if (selectionStart < 0) selectionStart = cursorPos;
        } else {
            clearSelection();
        }
        setCursorPos(bestPos);
        return true;
    }

    public void setVanillaFacade() {
        this.setBackgroundRenderFunc((gui, txt) -> {
            int bgColor = 0xFF101010;
            int borderColor = txt.focusing ? 0xFFA0A0A0 : 0xFF808080;
            gui.drawRect(txt.x(), txt.y(), txt.width(), txt.height(), borderColor);
            gui.drawRect(txt.x()+1, txt.y()+1, txt.width()-2, txt.height()-2, bgColor);
        });
        this.setRenderFunc((gui, txt) -> {
            int xOff = txt.x() + SPAN - txt.textOffsetX;
            int yOff = txt.y() + (txt.height() - GuiRenderer.TEXT_HEIGHT) / 2;

            //Selection box
            if (txt.hasSelection()) {
                int start = Math.min(txt.selectionStart, txt.cursorPos);
                int end = Math.max(txt.selectionStart, txt.cursorPos);
                int selX1 = xOff + gui.getTextWidth(txt.text.substring(0, start));
                int selX2 = xOff + gui.getTextWidth(txt.text.substring(0, end));
                gui.drawRect(selX1, yOff, selX2 - selX1, GuiRenderer.TEXT_HEIGHT, 0xFF0080FF);
            }

            gui.drawString(txt.text, xOff, yOff, 0xFFFFFF);

            //Cursor
            if (txt.focusing && txt.cursorBlinkTimer % 20 < 10) {
                int cursorX = xOff + gui.getTextWidth(txt.text.substring(0, txt.cursorPos));
                gui.drawRect(cursorX, yOff - 2 , 1, GuiRenderer.TEXT_HEIGHT + 4, 0xFFEEEEEE);
            }
        });
    }
}
