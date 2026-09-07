package com.omerfaruk.mesaitakvimi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(9, 13, 19);
    private static final int SURFACE = Color.rgb(18, 24, 33);
    private static final int SURFACE_ALT = Color.rgb(23, 31, 42);
    private static final int STROKE = Color.rgb(39, 51, 68);
    private static final int TEXT = Color.rgb(244, 247, 251);
    private static final int TEXT_MUTED = Color.rgb(159, 174, 194);
    private static final int ACCENT = Color.rgb(80, 157, 255);
    private static final int ACCENT_SOFT = Color.rgb(24, 48, 77);
    private static final int GREEN = Color.rgb(91, 214, 153);

    private static final String PREFS_NAME = "mesai_kayitlari";
    private static final String PREFIX_MINUTES = "m:";

    private final Locale tr = new Locale("tr", "TR");

    private Calendar shownMonth;
    private SharedPreferences prefs;
    private TextView monthTitle;
    private TextView totalText;
    private GridLayout calendarGrid;
    private LinearLayout calendarSection;
    private GestureDetector gestureDetector;
    private boolean monthAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        shownMonth = Calendar.getInstance(tr);
        shownMonth.set(Calendar.DAY_OF_MONTH, 1);

        gestureDetector = new GestureDetector(this, new SwipeListener());

        buildScreen();
        refreshCalendar();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
        }
        return super.dispatchTouchEvent(event);
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(18), dp(14), dp(12));
        root.setBackgroundColor(BG);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        top.addView(titleGroup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView appTitle = new TextView(this);
        appTitle.setText("Mesai Takvimi");
        appTitle.setTextColor(TEXT);
        appTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        appTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleGroup.addView(appTitle);

        TextView subtitle = new TextView(this);
        subtitle.setText("Aylık mesai kaydı");
        subtitle.setTextColor(TEXT_MUTED);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitle.setPadding(0, dp(2), 0, 0);
        titleGroup.addView(subtitle);

        TextView settings = squareButton("⚙");
        settings.setContentDescription("Ayarlar");
        settings.setOnClickListener(v -> showSettings());
        top.addView(settings);
        root.addView(top);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(0, dp(16), 0, dp(12));

        TextView prev = navButton("‹");
        prev.setContentDescription("Önceki ay");
        prev.setOnClickListener(v -> animateMonthChange(-1));
        nav.addView(prev);

        monthTitle = new TextView(this);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setTextColor(TEXT);
        monthTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        monthTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nav.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(46), 1f));

        TextView next = navButton("›");
        next.setContentDescription("Sonraki ay");
        next.setOnClickListener(v -> animateMonthChange(1));
        nav.addView(next);

        root.addView(nav);

        totalText = new TextView(this);
        totalText.setGravity(Gravity.CENTER_VERTICAL);
        totalText.setTextColor(TEXT);
        totalText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        totalText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        totalText.setPadding(dp(16), dp(14), dp(16), dp(14));
        totalText.setBackground(rounded(SURFACE_ALT, STROKE, 18, 1));
        root.addView(totalText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        calendarSection = new LinearLayout(this);
        calendarSection.setOrientation(LinearLayout.VERTICAL);

        LinearLayout weekHeader = new LinearLayout(this);
        weekHeader.setOrientation(LinearLayout.HORIZONTAL);
        weekHeader.setPadding(0, dp(14), 0, dp(5));
        String[] dayNames = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
        for (String name : dayNames) {
            TextView day = new TextView(this);
            day.setText(name);
            day.setGravity(Gravity.CENTER);
            day.setTextColor(TEXT_MUTED);
            day.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            day.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            weekHeader.addView(day, new LinearLayout.LayoutParams(0, dp(34), 1f));
        }
        calendarSection.addView(weekHeader);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, dp(8));

        calendarGrid = new GridLayout(this);
        calendarGrid.setColumnCount(7);
        calendarGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        calendarGrid.setUseDefaultMargins(false);
        scroll.addView(calendarGrid, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        calendarSection.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));

        root.addView(calendarSection, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));

        setContentView(root);
    }

    private void refreshCalendar() {
        calendarGrid.removeAllViews();

        String[] months = new DateFormatSymbols(tr).getMonths();
        String monthName = months[shownMonth.get(Calendar.MONTH)];
        monthTitle.setText(capitalize(monthName) + " " + shownMonth.get(Calendar.YEAR));

        Calendar first = (Calendar) shownMonth.clone();
        int mondayIndex = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;

        for (int i = 0; i < mondayIndex; i++) {
            addBlankCell();
        }

        int maxDay = shownMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        int totalMinutes = 0;

        for (int day = 1; day <= maxDay; day++) {
            String key = makeKey(shownMonth.get(Calendar.YEAR), shownMonth.get(Calendar.MONTH), day);
            int minutes = getStoredMinutes(key);
            totalMinutes += minutes;
            addDayCell(day, key, minutes);
        }

        int trailing = (7 - ((mondayIndex + maxDay) % 7)) % 7;
        for (int i = 0; i < trailing; i++) {
            addBlankCell();
        }

        totalText.setText("Bu ay toplam  •  " + formatTotalMinutes(totalMinutes));
    }

    private void addBlankCell() {
        View blank = new View(this);
        GridLayout.LayoutParams lp = cellParams();
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        blank.setLayoutParams(lp);
        blank.setBackground(rounded(Color.rgb(12, 17, 24), Color.rgb(26, 34, 46), 15, 1));
        calendarGrid.addView(blank);
    }

    private void addDayCell(int day, String key, int minutes) {
        boolean today = isToday(day);
        boolean hasHours = minutes > 0;

        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(3), dp(7), dp(3), dp(7));

        int fill = hasHours ? ACCENT_SOFT : SURFACE;
        int border = today ? ACCENT : (hasHours ? Color.rgb(54, 93, 137) : STROKE);
        int borderWidth = today ? 2 : 1;
        cell.setBackground(rounded(fill, border, 15, borderWidth));
        cell.setElevation(dp(1));

        TextView dayNo = new TextView(this);
        dayNo.setText(String.valueOf(day));
        dayNo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        dayNo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        dayNo.setTextColor(TEXT);
        dayNo.setGravity(Gravity.CENTER);
        cell.addView(dayNo);

        TextView hourText = new TextView(this);
        hourText.setText(hasHours ? formatDayMinutes(minutes) : "");
        hourText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hourText.setTextColor(hasHours ? GREEN : TEXT_MUTED);
        hourText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hourText.setGravity(Gravity.CENTER);
        hourText.setPadding(0, dp(6), 0, 0);
        cell.addView(hourText);

        cell.setOnClickListener(v -> showTimePicker(day, key, minutes));

        GridLayout.LayoutParams lp = cellParams();
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        cell.setLayoutParams(lp);
        calendarGrid.addView(cell);
    }

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(72);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        return lp;
    }

    private void showTimePicker(int day, String key, int currentMinutes) {
        String[] months = new DateFormatSymbols(tr).getMonths();
        String dateText = day + " " + capitalize(months[shownMonth.get(Calendar.MONTH)]) + " " + shownMonth.get(Calendar.YEAR);

        int initialHour = currentMinutes / 60;
        int initialMinute = currentMinutes % 60;

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                AlertDialog.THEME_DEVICE_DEFAULT_DARK,
                (view, hourOfDay, minute) -> {
                    int totalMinutes = hourOfDay * 60 + minute;
                    if (totalMinutes <= 0) {
                        prefs.edit().remove(key).apply();
                    } else {
                        prefs.edit().putString(key, PREFIX_MINUTES + totalMinutes).apply();
                    }
                    refreshCalendar();
                },
                initialHour,
                initialMinute,
                true
        );

        dialog.setTitle(dateText + " • Mesai süresi");
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Sil", (d, which) -> {
            prefs.edit().remove(key).apply();
            refreshCalendar();
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "İptal", (d, which) -> d.dismiss());
        dialog.show();
    }

    private void animateMonthChange(int amount) {
        if (monthAnimating || calendarSection == null) return;
        monthAnimating = true;

        int width = calendarSection.getWidth();
        if (width <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
        }

        final float outX = amount > 0 ? -width : width;
        final float inX = -outX;

        calendarSection.animate()
                .translationX(outX)
                .alpha(0.55f)
                .setDuration(170)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    shownMonth.add(Calendar.MONTH, amount);
                    shownMonth.set(Calendar.DAY_OF_MONTH, 1);
                    refreshCalendar();

                    calendarSection.setTranslationX(inX);
                    calendarSection.setAlpha(0.55f);
                    calendarSection.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(220)
                            .setInterpolator(new DecelerateInterpolator())
                            .withEndAction(() -> monthAnimating = false)
                            .start();
                })
                .start();
    }

    private void showSettings() {
        new AlertDialog.Builder(this)
                .setTitle("Ayarlar")
                .setMessage("Mesai Takvimi\n\nYapımcı: Ömer Faruk Boz\nSürüm: 1.2")
                .setPositiveButton("Tamam", null)
                .show();
    }

    private boolean isToday(int day) {
        Calendar today = Calendar.getInstance(tr);
        return shownMonth.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && shownMonth.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && day == today.get(Calendar.DAY_OF_MONTH);
    }

    private TextView squareButton(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(TEXT);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setBackground(rounded(SURFACE_ALT, STROKE, 14, 1));
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(46)));
        return view;
    }

    private TextView navButton(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(TEXT);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setBackground(rounded(SURFACE_ALT, STROKE, 14, 1));
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(46)));
        return view;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(strokeDp), stroke);
        return drawable;
    }

    private int getStoredMinutes(String key) {
        String raw = prefs.getString(key, "");
        if (TextUtils.isEmpty(raw)) return 0;

        try {
            if (raw.startsWith(PREFIX_MINUTES)) {
                return Integer.parseInt(raw.substring(PREFIX_MINUTES.length()));
            }

            // 1.1 ve önceki sürümlerdeki ondalık saat kayıtlarını koru.
            double hours = Double.parseDouble(raw.replace(',', '.'));
            if (hours <= 0) return 0;
            return (int) Math.round(hours * 60.0);
        } catch (Exception e) {
            return 0;
        }
    }

    private String makeKey(int year, int monthZeroBased, int day) {
        return String.format(tr, "%04d-%02d-%02d", year, monthZeroBased + 1, day);
    }

    private String formatDayMinutes(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (mins == 0) {
            return hours + " sa";
        }
        return String.format(tr, "%d:%02d sa", hours, mins);
    }

    private String formatTotalMinutes(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (mins == 0) {
            return hours + " saat";
        }
        return hours + " sa " + mins + " dk";
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase(tr) + value.substring(1);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class SwipeListener extends GestureDetector.SimpleOnGestureListener {
        private static final int MIN_DISTANCE = 90;
        private static final int MIN_VELOCITY = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)
                    && Math.abs(diffX) > MIN_DISTANCE
                    && Math.abs(velocityX) > MIN_VELOCITY) {
                animateMonthChange(diffX < 0 ? 1 : -1);
                return true;
            }
            return false;
        }
    }
}
