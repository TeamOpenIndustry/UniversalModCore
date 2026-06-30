package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.gui_v2.core.actions.*;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.input.Clipboard;
import cam72cam.mod.input.Keyboard;
import com.google.common.base.Predicates;

import java.util.function.Consumer;
import java.util.function.Predicate;

//TODO Multiline support
public class TextField extends AbstractWidget<TextField>
        implements IClickable, IFocusable, IDraggable, IKeyboardListener, IUpdatable {
    private static final int SPAN = GuiRenderer.TEXT_HEIGHT / 2;

    private String text;
    private Consumer<String> callback;
    private Predicate<String> validator = Predicates.alwaysTrue();
    private int cursorPos = 0;
    private int textOffsetX = 0;

    private boolean hasSelection = false;
    private int selectionStart = 0;

    private int cursorBlinkTimer = 0;

    private boolean focusing;

    protected TextField(int width, int height) {
        this.setBound(0, 0, width, height);
    }

    public static TextField of(int width, int height) {
        return new TextField(width, height);
    }

    public TextField callback(Consumer<String> callback) {
        this.callback = callback;
        return this;
    }

    public TextField text(String newText) {
        if (canApplyText(newText)) {
            this.text = newText;
            if (this.callback != null) {
                this.callback.accept(newText);
            }
        }
        return this;
    }

    public TextField validator(Predicate<String> validator) {
        this.validator = validator;
        return this;
    }

    public String getText() {
        return text;
    }

    public void setCursorPos(int newPos) {
        cursorPos = Math.max(0, Math.min(text.length(), newPos));
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

    public boolean hasSelection() {
        return hasSelection;
    }

    public String getSelectedText() {
        if (!hasSelection) return "";
        int start = Math.min(selectionStart, cursorPos);
        int end = Math.max(selectionStart, cursorPos);
        return text.substring(start, end);
    }

    @Override
    public void layout(int x, int y) {
        setX(x);
        setY(y);
    }

    @Override
    public boolean isFocusing() {
        return focusing;
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
        int currX = SPAN;
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
            if (!hasSelection) {
                hasSelection = true;
                selectionStart = cursorPos;
            }
        } else {
            clearSelection();
        }
        setCursorPos(bestPos);
        return true;
    }

    @Override
    public boolean onDrag(Player.Hand hand, int mouseX, int mouseY) {
        if (!focusing) return false;

        //Start dragging
        if (!hasSelection) {
            hasSelection = true;
            selectionStart = cursorPos;
        }

        int relativeX = mouseX - x() + textOffsetX;
        int bestPos = 0;
        int bestDist = Integer.MAX_VALUE;
        int currX = SPAN;
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
        setCursorPos(bestPos);

        //If cursor moves beyond then scroll the text to fit
        int localX = mouseX - x();
        int scrollThreshold = 10;
        if (localX < -scrollThreshold) {
            setCursorPos(cursorPos - 1);
        } else if (localX > width() + scrollThreshold) {
            setCursorPos(cursorPos + 1);
        }

        return true;
    }

    @Override
    public boolean onRelease(Player.Hand hand, int mouseX, int mouseY) {
        return false;
    }

    @Override
    public boolean onKeyPressed(Keyboard.KeyCode key) {
        if (!focusing) return false;

        switch (key) {
            case LEFT:
                if (Keyboard.isPressingShift()) {
                    if (!hasSelection) {
                        hasSelection = true;
                        selectionStart = cursorPos;
                    }
                } else {
                    clearSelection();
                }
                setCursorPos(cursorPos - 1);
                return true;
            case RIGHT:
                if (Keyboard.isPressingShift()) {
                    if (!hasSelection) {
                        hasSelection = true;
                        selectionStart = cursorPos;
                    }
                } else {
                    clearSelection();
                }
                setCursorPos(cursorPos + 1);
                return true;
            case HOME:
                if (Keyboard.isPressingShift()) {
                    if (!hasSelection) {
                        hasSelection = true;
                        selectionStart = cursorPos;
                    }
                } else {
                    clearSelection();
                }
                setCursorPos(0);
                return true;
            case END:
                if (Keyboard.isPressingShift()) {
                    if (!hasSelection) {
                        hasSelection = true;
                        selectionStart = cursorPos;
                    }
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
                        if (cursorPos == newText.length()) {
                            //Already at tail, don't move
                            setCursorPos(cursorPos);
                        } else {
                            //Move front
                            setCursorPos(cursorPos - 1);
                        }
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
                    hasSelection = true;
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
                        insert(clipboard);
                    }
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public void onTick() {
        cursorBlinkTimer ++;
    }

    @Override
    public boolean onCharTyped(char ch) {
        if (!focusing) return false;
        insert(ch);
        return true;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.clearSelection();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.clearSelection();
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

    private void clearSelection() {
        hasSelection = false;
        selectionStart = 0;
    }

    private void deleteSelection() {
        if (!hasSelection) return;
        int start = Math.min(selectionStart, cursorPos);
        int end = Math.max(selectionStart, cursorPos);
        String newText = text.substring(0, start) + text.substring(end);
        if (canApplyText(newText)) {
            setCursorPos(start);
            clearSelection();
        }
    }

    private void insert(char c) {
        insert(String.valueOf(c));
    }

    private void insert(String str) {
        deleteSelection();
        String newText = text.substring(0, cursorPos) + str + text.substring(cursorPos);
        if (canApplyText(newText)) {
            setCursorPos(cursorPos + str.length());
        }
    }

    public TextField vanilla() {
        return this.setRenderer((gui, txt) -> {
            int bgColor = 0xFF101010;
            int borderColor = txt.focusing ? 0xFFA0A0A0 : 0xFF808080;
            gui.drawRect(txt.x(), txt.y(), txt.width(), txt.height(), borderColor);
            gui.drawRect(txt.x()+1, txt.y()+1, txt.width()-2, txt.height()-2, bgColor);
            int xOff = txt.x() + SPAN - txt.textOffsetX;
            int yOff = txt.y() + (txt.height() - GuiRenderer.TEXT_HEIGHT) / 2;

            //Selection box
            if (txt.hasSelection()) {
                int start = Math.min(txt.selectionStart, txt.cursorPos);
                int end = Math.max(txt.selectionStart, txt.cursorPos);
                int selX1 = xOff + GuiUtils.getTextWidth(txt.text.substring(0, start));
                int selX2 = xOff + GuiUtils.getTextWidth(txt.text.substring(0, end));
                gui.drawRect(selX1, yOff, selX2 - selX1, GuiRenderer.TEXT_HEIGHT, 0xFF0080FF);
            }

            gui.drawString(txt.text, xOff, yOff, 0xFFFFFF);

            //Cursor
            if (txt.focusing && txt.cursorBlinkTimer % 20 < 10) {
                int cursorX = xOff + GuiUtils.getTextWidth(txt.text.substring(0, txt.cursorPos));
                gui.drawRect(cursorX, yOff - 2 , 1, GuiRenderer.TEXT_HEIGHT + 4, 0xFFEEEEEE);
            }
        });
    }
}