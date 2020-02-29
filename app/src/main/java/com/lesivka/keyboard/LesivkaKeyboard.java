package com.lesivka.keyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

public class LesivkaKeyboard extends InputMethodService implements
        KeyboardView.OnKeyboardActionListener {

    private static final String ACUTABLE = "bcčdđfghklmnprsštvxzžƶ";
    private static final String ACUTABLE_UPPER = ACUTABLE.toUpperCase();
    private static final long DOUBLE_PRESS_THRESHOLD = (long) 3e8;
    private static final int KEYCODE_SWITCH = -101;
    private static final int KEYCODE_ACUTE = 0x301;
    private static final String SHIFT_LABEL = "\u21e7";
    private static final String SHIFT_LABEL_LOCK = "\u21ea";

    private int lastCode;
    private long lastKeyPressed;
    private boolean capsLock;

    private KeyboardView kv;
    private Keyboard current;
    private Keyboard qwerty;
    private Keyboard symbols;
    private Key shift;

    private boolean acutable(String s) {
        return (!s.isEmpty() && (ACUTABLE.contains(s) || ACUTABLE_UPPER.contains(s)));
    }

    @Override
    public View onCreateInputView() {
        lastKeyPressed = System.nanoTime();
        capsLock = false;

        current = qwerty = new Keyboard(this, R.xml.qwerty);
        symbols = new Keyboard(this, R.xml.symbols);
        shift = qwerty.getKeys().get(qwerty.getShiftKeyIndex());

        kv = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);
        kv.setPreviewEnabled(false);
        kv.setKeyboard(current);
        kv.setOnKeyboardActionListener(this);

        return kv;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        long currentKeyPressed = System.nanoTime();
        boolean doubleKey = currentKeyPressed - lastKeyPressed < DOUBLE_PRESS_THRESHOLD;
        InputConnection ic = getCurrentInputConnection();
        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                ic.deleteSurroundingText(1, 0);
                break;
            case Keyboard.KEYCODE_SHIFT:
                boolean isShifted = kv.isShifted();
                if (doubleKey && isShifted) {
                    capsLock = true;
                    shift.label = SHIFT_LABEL_LOCK;
                    shift.on = true;
                } else {
                    capsLock = false;
                    shift.label = SHIFT_LABEL;
                    kv.setShifted(!isShifted);
                }
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            case Keyboard.KEYCODE_MODE_CHANGE:
                current = current == qwerty ? symbols : qwerty;
                kv.setKeyboard(current);
                break;
            case KEYCODE_SWITCH:
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
                break;
            case KEYCODE_ACUTE:
                String before = ic.getTextBeforeCursor(1, 0).toString();
                if (!acutable(before)) {
                    break;
                }
                ic.setComposingText("", 1);  // trick to fix an input of acute in Android Browser
            default:
                if (doubleKey && lastCode == primaryCode) {
                    onKey(KEYCODE_ACUTE, new int[] {});
                } else {
                    char code = (char) primaryCode;
                    if (Character.isLetter(code) && kv.isShifted()) {
                        code = Character.toUpperCase(code);
                        if (!capsLock) {
                            kv.setShifted(false);
                        }
                    }
                    ic.commitText(String.valueOf(code), 1);
                }
        }
        lastCode = primaryCode;
        lastKeyPressed = currentKeyPressed;
    }

    @Override
    public void onPress(int primaryCode) {}

    @Override
    public void onRelease(int primaryCode) {}

    @Override
    public void onText(CharSequence charSequence) {}

    @Override
    public void swipeLeft() {}

    @Override
    public void swipeRight() {}

    @Override
    public void swipeDown() {}

    @Override
    public void swipeUp() {}
}
