package com.lesivka.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.util.Timer;
import java.util.TimerTask;

public class LesivkaKeyboard extends InputMethodService implements
        KeyboardView.OnKeyboardActionListener {

    private static final String ACUTABLE = "bcčdđfghklmnprsštvxzžƶ";
    private static final String ACUTABLE_UPPER = ACUTABLE.toUpperCase();
    private static final int KEYCODE_SWITCH = -101;
    private static final int KEYCODE_ACUTE = 0x301;
    private static final int KEYCODE_SPACE = 0x20;
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
    private Vibrator v;
    private Timer longPressTimer;
    private boolean processing;

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

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        longPressTimer = new Timer();
        processing = true;

        return kv;
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(Settings.PREF_NAME, Settings.PREF_MODE);
    }

    private long getDoubleTapThreshold() {
        String s = getPreferences().getString(Settings.DOUBLE_TAP_DELAY, null);
        if (s == null) {
            return Settings.DOUBLE_TAP_DELAY_DEFAULT;
        }
        return Long.parseLong(s) * 1_000_000;
    }

    private boolean getAutoAcute() {
        return getPreferences().getBoolean(Settings.AUTO_ACUTE, Settings.AUTO_ACUTE_DEFAULT);
    }

    private boolean getVibrate() {
        return getPreferences().getBoolean(Settings.VIBRATE, Settings.VIBRATE_DEFAULT);
    }

    private void methodSwitch() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showInputMethodPicker();
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        if (!processing) {
            return;
        }
        long currentKeyPressed = System.nanoTime();
        boolean doubleKey = currentKeyPressed - lastKeyPressed < getDoubleTapThreshold();

        InputConnection ic = getCurrentInputConnection();
        String before = ic.getTextBeforeCursor(1, 0).toString();

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                ic.deleteSurroundingText(1, 0);
                break;
            case Keyboard.KEYCODE_SHIFT:
                if (doubleKey) {
                    capsLock = true;
                    shift.label = SHIFT_LABEL_LOCK;
                    shift.on = true;
                    kv.setShifted(true);
                } else {
                    capsLock = false;
                    shift.label = SHIFT_LABEL;
                    kv.setShifted(!kv.isShifted());
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
                methodSwitch();
                break;
            case KEYCODE_ACUTE:
                if (!acutable(before)) {
                    break;
                }
                ic.setComposingText("", 1);  // trick to fix an input of acute in Android Browser
            default:
                if (getAutoAcute() && doubleKey && lastCode == primaryCode && acutable(before)) {
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
    public void onPress(final int primaryCode) {
        if (getVibrate()) {
            v.vibrate(50);
        }

        longPressTimer.cancel();

        longPressTimer = new Timer();
        longPressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (primaryCode == KEYCODE_SPACE) {
                    processing = false;
                    LesivkaKeyboard.this.methodSwitch();
                }
            }
        }, ViewConfiguration.getLongPressTimeout());
    }

    @Override
    public void onRelease(int primaryCode) {
        longPressTimer.cancel();
        processing = true;
    }

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
