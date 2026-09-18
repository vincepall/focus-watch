package com.rt.focuswatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {

    // Colors
    private static final int COLOR_BG = 0xFF000000;
    private static final int COLOR_ACCENT = 0xFFF5DF4D;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_MUTED = 0xFFAAAAAA;
    private static final int COLOR_TEXT_DIM = 0xFF666666;
    private static final int COLOR_CANCEL_RED = 0xFFFF5252;

    // Calculation State (With requested defaults: D4 -> ALLE, FFD 80cm)
    private int currentMode = 0; // 0=Tijd, 1=mA*min, 2=Ci
    private int ffdNew = 80;
    private int ffdOld = 80;     // Default 80cm
    private int minutes = 2;
    private int seconds = 30;

    private String filmOld = "D4";    // Default D4
    private String filmNew = "ALLE";  // Default ALLE

    private double matFactor = 1.0;
    private String matName = "Geen factor";

    // mA*min state
    private double maminVal = 8.0;
    private double maVal = 3.0;

    // Ci state
    private int ciOldMin = 1;
    private int ciOldSec = 30;
    private double ciOldVal = 50.0;
    private double ciNewVal = 40.0;

    // Root Containers
    private FrameLayout rootFrame;
    private ScrollView mainScrollView;
    private LinearLayout mainContentLayout;

    // Mode Card Views
    private TextView[] btnModes = new TextView[3];
    private static final String[] MODE_NAMES = {"Tijd", "mA · min", "Ci Wissel"};

    // Time Card Views (Separated, Prominent, Touch to Edit)
    private TextView txtTimeBoxMin;
    private TextView txtTimeBoxSec;
    private TextView txtMaminBox;
    private TextView txtMaBox;
    private TextView txtCiOldMinBox;
    private TextView txtCiOldSecBox;
    private TextView txtCiOldValBox;
    private TextView txtCiNewValBox;
    private LinearLayout layoutTimeTijd;
    private LinearLayout layoutTimeMamin;
    private LinearLayout layoutTimeCi;

    // FFD Card Views
    private TextView txtFfdNewVal;
    private TextView txtFfdOldVal;

    // Film Card Views
    private TextView[] btnOldFilms = new TextView[3];
    private TextView[] btnNewFilms = new TextView[4];
    private static final String[] FILM_TYPES_OLD = {"D4", "D5", "D7"};
    private static final String[] FILM_TYPES_NEW = {"D4", "D5", "D7", "ALLE"};

    // Material Card Views
    private TextView txtMatFactorBox;
    private TextView txtMatSubLabel;

    // Result Card Views (At Bottom)
    private TextView txtResultTime;
    private TextView txtResultSub;
    private LinearLayout layoutMultiFilm;
    private TextView txtMultiD4;
    private TextView txtMultiD5;
    private TextView txtMultiD7;

    // Numpad Overlay
    private LinearLayout numpadOverlay;
    private TextView numpadTitle;
    private TextView numpadDisplay;
    private LinearLayout numpadRow4Integer;
    private LinearLayout numpadRow4Decimal;
    private String numpadBuffer = "";
    private int numpadTarget = 0;
    private boolean numpadIsDecimal = false;

    private static final int TARGET_FFD_NEW = 1;
    private static final int TARGET_FFD_OLD = 2;
    private static final int TARGET_MINUTES = 3;
    private static final int TARGET_SECONDS = 4;
    private static final int TARGET_MAMIN = 5;
    private static final int TARGET_MA = 6;
    private static final int TARGET_CI_OLD_VAL = 7;
    private static final int TARGET_CI_NEW_VAL = 8;
    private static final int TARGET_CI_OLD_MIN = 9;
    private static final int TARGET_CI_OLD_SEC = 10;
    private static final int TARGET_FACTOR = 11;

    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Init Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(COLOR_BG);

        // Main Scrollable List
        mainScrollView = new ScrollView(this);
        mainScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mainScrollView.setVerticalScrollBarEnabled(false);

        mainContentLayout = buildMainContent();
        mainScrollView.addView(mainContentLayout);
        rootFrame.addView(mainScrollView);

        // Fullscreen Numpad Overlay (Fits circular screen with guaranteed non-overlapping keys)
        numpadOverlay = buildFullscreenNumpad();
        numpadOverlay.setVisibility(View.GONE);
        rootFrame.addView(numpadOverlay);

        setContentView(rootFrame);

        updateAllUI();
    }

    private void haptic(long ms) {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(ms);
                }
            }
        } catch (Exception ignored) {}
    }

    private int dp(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // =========================================================================
    // MAIN CONTENT BUILDER: Mode -> Inputs (Pure Numbers) -> Result at Bottom
    // =========================================================================
    private LinearLayout buildMainContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        // Safe padding for circular bezel
        root.setPadding(dp(12), dp(32), dp(12), dp(56));
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 1. INVOERMODUS CARD (TOP)
        root.addView(buildModeCard());
        addSpace(root, dp(8));

        // 2. BEGINTIJD CARD (Minuten & Seconden apart)
        root.addView(buildTimeCard());
        addSpace(root, dp(8));

        // 3. FFD AFSTAND CARD (Default 80cm)
        root.addView(buildFfdCard());
        addSpace(root, dp(8));

        // 4. FILMSOORT CARD (Default D4 -> ALLE)
        root.addView(buildFilmCard());
        addSpace(root, dp(8));

        // 5. MATERIAALFACTOR CARD
        root.addView(buildMaterialCard());
        addSpace(root, dp(10));

        // 6. NIEUWE STRALINGSTIJD (RESULT CARD AT THE VERY BOTTOM)
        root.addView(buildResultCard());

        return root;
    }

    // =========================================================================
    // 1. INVOERMODUS CARD
    // =========================================================================
    private View buildModeCard() {
        LinearLayout card = createBaseCard("INVOERMODUS");

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        for (int i = 0; i < MODE_NAMES.length; i++) {
            final int modeIdx = i;
            TextView b = createSegmentBtn(MODE_NAMES[i], new View.OnClickListener() {
                @Override public void onClick(View v) {
                    haptic(20);
                    currentMode = modeIdx;
                    updateAllUI();
                }
            });
            btnModes[i] = b;
            row.addView(b);
        }
        card.addView(row);

        return card;
    }

    // =========================================================================
    // 2. TIME CARD: MINUTEN EN SECONDEN APART INVOEREN (VOLLEDIGE BREEDTE)
    // =========================================================================
    private View buildTimeCard() {
        LinearLayout card = createBaseCard("BEGINTIJD");

        // MODE 0: TIJD (Minuten & Seconden als twee duidelijke rijen)
        layoutTimeTijd = new LinearLayout(this);
        layoutTimeTijd.setOrientation(LinearLayout.VERTICAL);

        // Rij 1: MINUTEN
        txtTimeBoxMin = new TextView(this);
        LinearLayout rowMin = createFullWidthValueRow("MINUTEN:", txtTimeBoxMin, COLOR_ACCENT, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("MINUTEN", minutes, TARGET_MINUTES, false);
            }
        });
        layoutTimeTijd.addView(rowMin);

        addSpace(layoutTimeTijd, dp(6));

        // Rij 2: SECONDEN
        txtTimeBoxSec = new TextView(this);
        LinearLayout rowSec = createFullWidthValueRow("SECONDEN:", txtTimeBoxSec, COLOR_ACCENT, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("SECONDEN", seconds, TARGET_SECONDS, false);
            }
        });
        layoutTimeTijd.addView(rowSec);

        card.addView(layoutTimeTijd);

        // MODE 1: mA * min
        layoutTimeMamin = new LinearLayout(this);
        layoutTimeMamin.setOrientation(LinearLayout.VERTICAL);
        layoutTimeMamin.setVisibility(View.GONE);

        txtMaminBox = new TextView(this);
        LinearLayout rowMm = createFullWidthValueRow("mA · MINUUT:", txtMaminBox, COLOR_ACCENT, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("mA · MINUUT", maminVal, TARGET_MAMIN, true);
            }
        });
        layoutTimeMamin.addView(rowMm);

        addSpace(layoutTimeMamin, dp(6));

        txtMaBox = new TextView(this);
        LinearLayout rowMa = createFullWidthValueRow("mA (STROOM):", txtMaBox, COLOR_ACCENT, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("mA (STROOM)", maVal, TARGET_MA, true);
            }
        });
        layoutTimeMamin.addView(rowMa);

        card.addView(layoutTimeMamin);

        // MODE 2: Ci Wissel
        layoutTimeCi = new LinearLayout(this);
        layoutTimeCi.setOrientation(LinearLayout.VERTICAL);
        layoutTimeCi.setVisibility(View.GONE);

        txtCiOldMinBox = new TextView(this);
        LinearLayout rowCiMin = createFullWidthValueRow("OUDE MINUTEN:", txtCiOldMinBox, COLOR_TEXT_WHITE, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("OUDE MINUTEN", ciOldMin, TARGET_CI_OLD_MIN, false);
            }
        });
        layoutTimeCi.addView(rowCiMin);

        addSpace(layoutTimeCi, dp(4));

        txtCiOldSecBox = new TextView(this);
        LinearLayout rowCiSec = createFullWidthValueRow("OUDE SECONDEN:", txtCiOldSecBox, COLOR_TEXT_WHITE, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("OUDE SECONDEN", ciOldSec, TARGET_CI_OLD_SEC, false);
            }
        });
        layoutTimeCi.addView(rowCiSec);

        addSpace(layoutTimeCi, dp(4));

        txtCiOldValBox = new TextView(this);
        LinearLayout rowCi1 = createFullWidthValueRow("OUDE CI:", txtCiOldValBox, COLOR_TEXT_WHITE, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("OUDE CI", ciOldVal, TARGET_CI_OLD_VAL, true);
            }
        });
        layoutTimeCi.addView(rowCi1);

        addSpace(layoutTimeCi, dp(4));

        txtCiNewValBox = new TextView(this);
        LinearLayout rowCi2 = createFullWidthValueRow("NIEUWE CI:", txtCiNewValBox, COLOR_ACCENT, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("NIEUWE CI", ciNewVal, TARGET_CI_NEW_VAL, true);
            }
        });
        layoutTimeCi.addView(rowCi2);

        card.addView(layoutTimeCi);

        return card;
    }

    // =========================================================================
    // 3. FFD CARD: STANDAARD 80 CM (VOLLEDIGE BREEDTE RIJEN)
    // =========================================================================
    private View buildFfdCard() {
        LinearLayout card = createBaseCard("FFD AFSTAND (cm)");

        txtFfdNewVal = new TextView(this);
        LinearLayout rowNew = createFullWidthValueRow("NIEUWE FFD:", txtFfdNewVal, COLOR_ACCENT, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("NIEUWE FFD (cm)", ffdNew, TARGET_FFD_NEW, false);
            }
        });
        card.addView(rowNew);

        addSpace(card, dp(6));

        txtFfdOldVal = new TextView(this);
        LinearLayout rowOld = createFullWidthValueRow("OUDE FFD:", txtFfdOldVal, COLOR_TEXT_WHITE, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("OUDE FFD (cm)", ffdOld, TARGET_FFD_OLD, false);
            }
        });
        card.addView(rowOld);

        return card;
    }

    // =========================================================================
    // 4. FILM CARD: STANDAARD D4 NAAR ALLE
    // =========================================================================
    private View buildFilmCard() {
        LinearLayout card = createBaseCard("FILMSOORT (CONVERSIE)");

        TextView lblFrom = new TextView(this);
        lblFrom.setText("VAN (REFERENTIE):");
        lblFrom.setTextSize(10);
        lblFrom.setTextColor(COLOR_TEXT_MUTED);
        lblFrom.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(lblFrom);

        LinearLayout rowFrom = new LinearLayout(this);
        rowFrom.setOrientation(LinearLayout.HORIZONTAL);
        rowFrom.setBaselineAligned(false);
        rowFrom.setGravity(Gravity.CENTER_VERTICAL);
        rowFrom.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
        for (int i = 0; i < FILM_TYPES_OLD.length; i++) {
            final String f = FILM_TYPES_OLD[i];
            TextView b = createSegmentBtn(f, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    haptic(15);
                    filmOld = f;
                    updateAllUI();
                }
            });
            btnOldFilms[i] = b;
            rowFrom.addView(b);
        }
        card.addView(rowFrom);

        addSpace(card, dp(6));

        TextView lblTo = new TextView(this);
        lblTo.setText("NAAR (NIEUW):");
        lblTo.setTextSize(10);
        lblTo.setTextColor(COLOR_ACCENT);
        lblTo.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(lblTo);

        LinearLayout rowTo = new LinearLayout(this);
        rowTo.setOrientation(LinearLayout.HORIZONTAL);
        rowTo.setBaselineAligned(false);
        rowTo.setGravity(Gravity.CENTER_VERTICAL);
        rowTo.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
        for (int i = 0; i < FILM_TYPES_NEW.length; i++) {
            final String f = FILM_TYPES_NEW[i];
            TextView b = createSegmentBtn(f, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    haptic(15);
                    filmNew = f;
                    updateAllUI();
                }
            });
            btnNewFilms[i] = b;
            rowTo.addView(b);
        }
        card.addView(rowTo);

        return card;
    }

    // =========================================================================
    // 5. MATERIAL CARD
    // =========================================================================
    private View buildMaterialCard() {
        LinearLayout card = createBaseCard("MATERIAALFACTOR");

        txtMatFactorBox = new TextView(this);
        LinearLayout rowMat = createFullWidthValueRow("FACTOR:", txtMatFactorBox, COLOR_ACCENT, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("MATERIAALFACTOR", matFactor, TARGET_FACTOR, true);
            }
        });
        card.addView(rowMat);

        addSpace(card, dp(4));

        txtMatSubLabel = new TextView(this);
        txtMatSubLabel.setText("Geen factor (1.00x)");
        txtMatSubLabel.setTextSize(11);
        txtMatSubLabel.setTextColor(COLOR_TEXT_MUTED);
        txtMatSubLabel.setGravity(Gravity.CENTER);
        card.addView(txtMatSubLabel);

        addSpace(card, dp(4));

        Button btnPickDb = new Button(this);
        btnPickDb.setText("Kies legering uit database...");
        btnPickDb.setTextSize(11);
        btnPickDb.setTextColor(COLOR_TEXT_WHITE);
        btnPickDb.setBackgroundResource(R.drawable.btn_key_normal);
        btnPickDb.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
        btnPickDb.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showMaterialPicker();
            }
        });
        card.addView(btnPickDb);

        return card;
    }

    private void showMaterialPicker() {
        final String[] items = new String[MaterialDb.ALL.length];
        for (int i = 0; i < MaterialDb.ALL.length; i++) {
            MaterialDb.Material m = MaterialDb.ALL[i];
            items[i] = m.name + " (" + String.format(Locale.US, "%.2f", m.f300) + "x)";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecteer Legering");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                MaterialDb.Material m = MaterialDb.ALL[which];
                matFactor = m.f300;
                matName = m.name;
                updateAllUI();
            }
        });
        builder.show();
    }

    // =========================================================================
    // 6. RESULT CARD (AT THE VERY BOTTOM)
    // =========================================================================
    private View buildResultCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.result_card_bg);
        card.setPadding(dp(12), dp(12), dp(12), dp(14));
        card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tag = new TextView(this);
        tag.setText("NIEUWE STRALINGSTIJD");
        tag.setTextSize(11);
        tag.setTextColor(COLOR_ACCENT);
        tag.setTypeface(Typeface.DEFAULT_BOLD);
        tag.setGravity(Gravity.CENTER);
        card.addView(tag);

        txtResultTime = new TextView(this);
        txtResultTime.setText("2m 30s");
        txtResultTime.setTextSize(36);
        txtResultTime.setTextColor(COLOR_TEXT_WHITE);
        txtResultTime.setTypeface(Typeface.DEFAULT_BOLD);
        txtResultTime.setGravity(Gravity.CENTER);
        card.addView(txtResultTime);

        // Multi Film Container (Visible by default since filmNew is "ALLE")
        layoutMultiFilm = new LinearLayout(this);
        layoutMultiFilm.setOrientation(LinearLayout.VERTICAL);
        layoutMultiFilm.setPadding(0, dp(4), 0, dp(4));

        txtMultiD4 = createMultiFilmRow(layoutMultiFilm, "D4 Film");
        txtMultiD5 = createMultiFilmRow(layoutMultiFilm, "D5 Film");
        txtMultiD7 = createMultiFilmRow(layoutMultiFilm, "D7 Film");
        card.addView(layoutMultiFilm);

        txtResultSub = new TextView(this);
        txtResultSub.setTextSize(11);
        txtResultSub.setTextColor(COLOR_TEXT_MUTED);
        txtResultSub.setGravity(Gravity.CENTER);
        card.addView(txtResultSub);

        return card;
    }

    private TextView createMultiFilmRow(LinearLayout parent, String name) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(2), 0, dp(2));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView lbl = new TextView(this);
        lbl.setText(name);
        lbl.setTextSize(13);
        lbl.setTextColor(COLOR_ACCENT);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView val = new TextView(this);
        val.setText("0m 00s");
        val.setTextSize(14);
        val.setTextColor(COLOR_TEXT_WHITE);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        val.setGravity(Gravity.END);

        row.addView(lbl);
        row.addView(val);
        parent.addView(row);
        return val;
    }

    // =========================================================================
    // FULLSCREEN NUMPAD OVERLAY (PERFECTLY SCALED FOR ROUND WEAR OS SCREEN)
    // =========================================================================
    private LinearLayout buildFullscreenNumpad() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(COLOR_BG);
        // Safe padding for circular bezel
        root.setPadding(dp(8), dp(10), dp(8), dp(6));
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Header Title
        numpadTitle = new TextView(this);
        numpadTitle.setText("MINUTEN (Huidig: 2)");
        numpadTitle.setTextSize(11);
        numpadTitle.setTextColor(COLOR_ACCENT);
        numpadTitle.setTypeface(Typeface.DEFAULT_BOLD);
        numpadTitle.setGravity(Gravity.CENTER);
        root.addView(numpadTitle);

        // 2. Display Row: [ Value Display ] and [ Backspace ⌫ ]
        LinearLayout displayRow = new LinearLayout(this);
        displayRow.setOrientation(LinearLayout.HORIZONTAL);
        displayRow.setGravity(Gravity.CENTER);
        displayRow.setLayoutParams(new LinearLayout.LayoutParams(dp(156), ViewGroup.LayoutParams.WRAP_CONTENT));

        numpadDisplay = new TextView(this);
        numpadDisplay.setText("_");
        numpadDisplay.setTextSize(22);
        numpadDisplay.setTextColor(COLOR_TEXT_WHITE);
        numpadDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        numpadDisplay.setGravity(Gravity.CENTER);
        numpadDisplay.setPadding(0, 0, 0, 0);
        numpadDisplay.setIncludeFontPadding(false);
        numpadDisplay.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        displayRow.addView(numpadDisplay);

        TextView btnBk = new TextView(this);
        btnBk.setText("⌫");
        btnBk.setTextSize(16);
        btnBk.setTextColor(COLOR_TEXT_WHITE);
        btnBk.setTypeface(Typeface.DEFAULT_BOLD);
        btnBk.setGravity(Gravity.CENTER);
        btnBk.setPadding(0, 0, 0, 0);
        btnBk.setIncludeFontPadding(false);
        btnBk.setBackgroundResource(R.drawable.btn_key_normal);
        btnBk.setClickable(true);
        btnBk.setFocusable(true);
        LinearLayout.LayoutParams bkLp = new LinearLayout.LayoutParams(dp(38), dp(26));
        btnBk.setLayoutParams(bkLp);
        btnBk.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(15);
                handleNumpadKey("⌫");
            }
        });
        displayRow.addView(btnBk);
        root.addView(displayRow);

        addSpace(root, dp(3));

        // 3. Keypad Container: Exactly 156dp wide to guarantee circular fit & zero overlap!
        LinearLayout keyGrid = new LinearLayout(this);
        keyGrid.setOrientation(LinearLayout.VERTICAL);
        keyGrid.setGravity(Gravity.CENTER);
        keyGrid.setLayoutParams(new LinearLayout.LayoutParams(dp(156), ViewGroup.LayoutParams.WRAP_CONTENT));

        // Rows 1, 2, 3: Digits 1 to 9 (3 keys per row, weighted equally)
        String[][] digitRows = {
            {"1", "2", "3"},
            {"4", "5", "6"},
            {"7", "8", "9"}
        };

        for (final String[] rowKeys : digitRows) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(35)));

            for (final String key : rowKeys) {
                TextView b = createNumpadKey(key, 20, COLOR_TEXT_WHITE, R.drawable.btn_key_normal, 1.0f, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        handleNumpadKey(key);
                    }
                });
                row.addView(b);
            }
            keyGrid.addView(row);
        }

        // Row 4 for Integers: [ TERUG ]   [ 0 ]   [ OK ]
        numpadRow4Integer = new LinearLayout(this);
        numpadRow4Integer.setOrientation(LinearLayout.HORIZONTAL);
        numpadRow4Integer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(35)));

        TextView btnIntCancel = createNumpadKey("TERUG", 12, COLOR_CANCEL_RED, R.drawable.btn_key_cancel, 1.15f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                numpadOverlay.setVisibility(View.GONE);
            }
        });

        TextView btnInt0 = createNumpadKey("0", 20, COLOR_TEXT_WHITE, R.drawable.btn_key_normal, 1.0f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                handleNumpadKey("0");
            }
        });

        TextView btnIntConfirm = createNumpadKey("OK", 14, 0xFF000000, R.drawable.btn_key_confirm, 1.15f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                handleNumpadKey("✓");
            }
        });

        numpadRow4Integer.addView(btnIntCancel);
        numpadRow4Integer.addView(btnInt0);
        numpadRow4Integer.addView(btnIntConfirm);
        keyGrid.addView(numpadRow4Integer);

        // Row 4 for Decimals: [ ✕ ]  [ 0 ]  [ . ]  [ ✓ ]
        numpadRow4Decimal = new LinearLayout(this);
        numpadRow4Decimal.setOrientation(LinearLayout.HORIZONTAL);
        numpadRow4Decimal.setVisibility(View.GONE);
        numpadRow4Decimal.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(35)));

        TextView btnDecCancel = createNumpadKey("✕", 16, COLOR_CANCEL_RED, R.drawable.btn_key_cancel, 1.0f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                numpadOverlay.setVisibility(View.GONE);
            }
        });

        TextView btnDec0 = createNumpadKey("0", 20, COLOR_TEXT_WHITE, R.drawable.btn_key_normal, 1.0f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                handleNumpadKey("0");
            }
        });

        TextView btnDecDot = createNumpadKey(".", 22, COLOR_TEXT_WHITE, R.drawable.btn_key_normal, 1.0f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                handleNumpadKey(".");
            }
        });

        TextView btnDecConfirm = createNumpadKey("✓", 18, 0xFF000000, R.drawable.btn_key_confirm, 1.0f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                handleNumpadKey("✓");
            }
        });

        numpadRow4Decimal.addView(btnDecCancel);
        numpadRow4Decimal.addView(btnDec0);
        numpadRow4Decimal.addView(btnDecDot);
        numpadRow4Decimal.addView(btnDecConfirm);
        keyGrid.addView(numpadRow4Decimal);

        root.addView(keyGrid);
        return root;
    }

    private TextView createNumpadKey(final String text, int textSizeSp, int textColor, int bgRes, float weight, final View.OnClickListener onClick) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(textSizeSp);
        tv.setTextColor(textColor);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 0, 0, 0);
        tv.setIncludeFontPadding(false);
        tv.setBackgroundResource(bgRes);
        tv.setClickable(true);
        tv.setFocusable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(2), dp(1), dp(2), dp(1));
        tv.setLayoutParams(lp);

        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(15);
                onClick.onClick(v);
            }
        });
        return tv;
    }

    private void openNumpad(String title, double currentVal, int target, boolean isDecimal) {
        haptic(20);
        numpadTarget = target;
        numpadIsDecimal = isDecimal;
        String curStr = isDecimal ? String.format(Locale.US, "%.2f", currentVal) : String.valueOf((int) Math.round(currentVal));
        numpadTitle.setText(title + " (Huidig: " + curStr + ")");
        numpadBuffer = "";
        numpadDisplay.setText("_");

        numpadRow4Integer.setVisibility(isDecimal ? View.GONE : View.VISIBLE);
        numpadRow4Decimal.setVisibility(isDecimal ? View.VISIBLE : View.GONE);

        numpadOverlay.setVisibility(View.VISIBLE);
    }

    private void handleNumpadKey(String key) {
        if (key.equals("✓")) {
            applyNumpadValue();
            numpadOverlay.setVisibility(View.GONE);
            updateAllUI();
            return;
        }
        if (key.equals("⌫")) {
            if (numpadBuffer.length() > 0) {
                numpadBuffer = numpadBuffer.substring(0, numpadBuffer.length() - 1);
            }
        } else if (key.equals(".")) {
            if (!numpadBuffer.contains(".") && numpadBuffer.length() < 7) {
                numpadBuffer = numpadBuffer.isEmpty() ? "0." : numpadBuffer + ".";
            }
        } else {
            if (numpadBuffer.length() < 7) {
                numpadBuffer += key;
            }
        }
        numpadDisplay.setText(numpadBuffer.isEmpty() ? "_" : numpadBuffer);
    }

    private void applyNumpadValue() {
        if (numpadBuffer.isEmpty()) {
            return; // keep current value
        }
        try {
            double v = Double.parseDouble(numpadBuffer);
            int iv = (int) Math.round(v);
            switch (numpadTarget) {
                case TARGET_FFD_NEW: ffdNew = Math.max(1, iv); break;
                case TARGET_FFD_OLD: ffdOld = Math.max(1, iv); break;
                case TARGET_MINUTES: minutes = Math.max(0, iv); break;
                case TARGET_SECONDS: seconds = Math.max(0, Math.min(59, iv)); break;
                case TARGET_MAMIN: maminVal = Math.max(0.1, v); break;
                case TARGET_MA: maVal = Math.max(0.1, v); break;
                case TARGET_CI_OLD_VAL: ciOldVal = Math.max(0.1, v); break;
                case TARGET_CI_NEW_VAL: ciNewVal = Math.max(0.1, v); break;
                case TARGET_CI_OLD_MIN: ciOldMin = Math.max(0, iv); break;
                case TARGET_CI_OLD_SEC: ciOldSec = Math.max(0, Math.min(59, iv)); break;
                case TARGET_FACTOR: matFactor = Math.max(0.01, v); matName = "Handmatig"; break;
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // UI UPDATES & CALCULATIONS
    // =========================================================================
    private void updateAllUI() {
        // 1. Result Card Display (At bottom)
        double resSec = calculateResultSecondsForFilm(filmNew.equals("ALLE") ? filmOld : filmNew);

        if (filmNew.equals("ALLE")) {
            txtResultTime.setVisibility(View.GONE);
            layoutMultiFilm.setVisibility(View.VISIBLE);
            txtMultiD4.setText(formatTime(calculateResultSecondsForFilm("D4")));
            txtMultiD5.setText(formatTime(calculateResultSecondsForFilm("D5")));
            txtMultiD7.setText(formatTime(calculateResultSecondsForFilm("D7")));
        } else {
            txtResultTime.setVisibility(View.VISIBLE);
            layoutMultiFilm.setVisibility(View.GONE);
            txtResultTime.setText(formatTime(resSec));
        }

        // Subtitle detail
        double fRatio = (ffdOld > 0) ? ((double) ffdNew / (double) ffdOld) : 1.0;
        double ffdFactor = fRatio * fRatio;
        String sub = String.format(Locale.US, "FFD: %d→%dcm (%.2fx)", ffdOld, ffdNew, ffdFactor);
        if (matFactor != 1.0) {
            sub += String.format(Locale.US, " • Mat: %.2fx", matFactor);
        }
        txtResultSub.setText(sub);

        // 2. Mode Buttons
        for (int i = 0; i < MODE_NAMES.length; i++) {
            setSegmentStyle(btnModes[i], i == currentMode);
        }

        // 3. Time Values (Separated: Minuten & Seconden)
        layoutTimeTijd.setVisibility(currentMode == 0 ? View.VISIBLE : View.GONE);
        layoutTimeMamin.setVisibility(currentMode == 1 ? View.VISIBLE : View.GONE);
        layoutTimeCi.setVisibility(currentMode == 2 ? View.VISIBLE : View.GONE);

        txtTimeBoxMin.setText(String.format(Locale.US, "%d m", minutes));
        txtTimeBoxSec.setText(String.format(Locale.US, "%d s", seconds));

        txtMaminBox.setText(String.format(Locale.US, "%.1f", maminVal));
        txtMaBox.setText(String.format(Locale.US, "%.1f", maVal));

        txtCiOldMinBox.setText(String.format(Locale.US, "%d m", ciOldMin));
        txtCiOldSecBox.setText(String.format(Locale.US, "%d s", ciOldSec));
        txtCiOldValBox.setText(String.format(Locale.US, "%.1f Ci", ciOldVal));
        txtCiNewValBox.setText(String.format(Locale.US, "%.1f Ci", ciNewVal));

        // 4. FFD Values (Default 80cm)
        txtFfdNewVal.setText(String.format(Locale.US, "%d cm", ffdNew));
        txtFfdOldVal.setText(String.format(Locale.US, "%d cm", ffdOld));

        // 5. Film Buttons (Default D4 -> ALLE)
        for (int i = 0; i < FILM_TYPES_OLD.length; i++) {
            boolean active = FILM_TYPES_OLD[i].equals(filmOld);
            setSegmentStyle(btnOldFilms[i], active);
        }
        for (int i = 0; i < FILM_TYPES_NEW.length; i++) {
            boolean active = FILM_TYPES_NEW[i].equals(filmNew);
            setSegmentStyle(btnNewFilms[i], active);
        }

        // 6. Material Values
        txtMatFactorBox.setText(String.format(Locale.US, "%.2f x", matFactor));
        txtMatSubLabel.setText(matName + " (" + String.format(Locale.US, "%.2f", matFactor) + "x)");
    }

    private double calculateBaseSeconds() {
        if (currentMode == 0) {
            return (minutes * 60) + seconds;
        } else if (currentMode == 1) {
            if (maVal <= 0) return 0;
            return (maminVal / maVal) * 60.0;
        } else {
            if (ciNewVal <= 0) return 0;
            double oldTime = (ciOldMin * 60) + ciOldSec;
            return oldTime * (ciOldVal / ciNewVal);
        }
    }

    private double calculateResultSecondsForFilm(String targetFilm) {
        double base = calculateBaseSeconds();
        if (base <= 0) return 0;

        // FFD inverse square law
        if (ffdOld > 0 && ffdNew > 0) {
            double ratio = (double) ffdNew / (double) ffdOld;
            base = base * (ratio * ratio);
        }

        // Material Factor
        base = base * matFactor;

        // Film Factor
        if (!filmOld.equals(targetFilm)) {
            base = convertFilmTime(base, filmOld, targetFilm);
        }

        return base;
    }

    private double convertFilmTime(double time, String from, String to) {
        if (from.equals(to)) return time;
        if (from.equals("D7")) {
            if (to.equals("D4")) return time * 2.44;
            if (to.equals("D5")) return time * (2.44 / 1.6666);
        } else if (from.equals("D4")) {
            if (to.equals("D5")) return time / 1.6666;
            if (to.equals("D7")) return time / 2.44;
        } else if (from.equals("D5")) {
            if (to.equals("D4")) return time * 1.6666;
            if (to.equals("D7")) return (time * 1.6666) / 2.44;
        }
        return time;
    }

    private String formatTime(double totalSec) {
        if (totalSec <= 0) return "0m 00s";
        long rounded = Math.round(totalSec);
        long m = rounded / 60;
        long s = rounded % 60;
        if (m >= 60) {
            long h = m / 60;
            m = m % 60;
            return String.format(Locale.US, "%dh %02dm %02ds", h, m, s);
        }
        return String.format(Locale.US, "%dm %02ds", m, s);
    }

    // =========================================================================
    // ROTARY CROWN SCROLLING (PIXEL WATCH 3)
    // =========================================================================
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            float delta = -event.getAxisValue(MotionEvent.AXIS_SCROLL);
            if (delta != 0 && numpadOverlay.getVisibility() != View.VISIBLE) {
                mainScrollView.scrollBy(0, (int) (delta * dp(45)));
                haptic(5);
                return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (numpadOverlay != null && numpadOverlay.getVisibility() == View.VISIBLE) {
            numpadOverlay.setVisibility(View.GONE);
            return;
        }
        super.onBackPressed();
    }

    // =========================================================================
    // VIEW HELPERS FOR CLEAN WEAR OS FULL-WIDTH TOUCH ROWS
    // =========================================================================
    private LinearLayout createBaseCard(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(10);
        t.setTextColor(COLOR_TEXT_DIM);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(t);
        addSpace(card, dp(6));

        return card;
    }

    private LinearLayout createFullWidthValueRow(String label, TextView valueView, int valueColor, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.card_touch_val);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(12);
        lbl.setTextColor(COLOR_TEXT_MUTED);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        valueView.setTextSize(20);
        valueView.setTextColor(valueColor);
        valueView.setTypeface(Typeface.DEFAULT_BOLD);
        valueView.setGravity(Gravity.END);

        row.addView(lbl);
        row.addView(valueView);

        final View.OnClickListener click = onClick;
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(20);
                click.onClick(v);
            }
        });
        return row;
    }

    private TextView createSegmentBtn(String text, View.OnClickListener onClick) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(11);
        b.setSingleLine(true);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        b.setIncludeFontPadding(false);
        b.setClickable(true);
        b.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        b.setLayoutParams(lp);
        b.setOnClickListener(onClick);
        return b;
    }

    private void setSegmentStyle(TextView b, boolean active) {
        if (active) {
            b.setBackgroundResource(R.drawable.btn_pill_active);
            b.setTextColor(0xFF111111);
        } else {
            b.setBackgroundResource(R.drawable.btn_pill_inactive);
            b.setTextColor(COLOR_TEXT_WHITE);
        }
    }

    private void addSpace(ViewGroup parent, int sizePx) {
        View space = new View(this);
        space.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, sizePx));
        parent.addView(space);
    }
}
