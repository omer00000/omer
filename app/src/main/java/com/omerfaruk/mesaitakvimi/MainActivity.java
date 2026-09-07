package com.omerfaruk.mesaitakvimi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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

    private final Locale tr = new Locale("tr", "TR");
    private Calendar shownMonth;
    private SharedPreferences prefs;
    private TextView monthTitle;
    private TextView totalText;
    private GridLayout calendarGrid;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("mesai_kayitlari", MODE_PRIVATE);
        shownMonth = Calendar.getInstance(tr);
        shownMonth.set(Calendar.DAY_OF_MONTH, 1);

        gestureDetector = new GestureDetector(this, new SwipeListener());

        buildScreen();
        refreshCalendar();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
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
        prev.setOnClickListener(v -> changeMonth(-1));
        nav.addView(prev);

        monthTitle = new TextView(this);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setTextColor(TEXT);
        monthTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        monthTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nav.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(46), 1f));

        TextView next = navButton("›");
        next.setContentDescription("Sonraki ay");
        next.setOnClickListener(v -> changeMonth(1));
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
        root.addView(weekHeader);

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

        root.addView(scroll, new LinearLayout.LayoutParams(
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
        double total = 0;

        for (int day = 1; day <= maxDay; day++) {
            String key = makeKey(shownMonth.get(Calendar.YEAR), shownMonth.get(Calendar.MONTH), day);
            double hours = getHours(key);
            total += hours;
            addDayCell(day, key, hours);
        }

        int trailing = (7 - ((mondayIndex + maxDay) % 7)) % 7;
        for (int i = 0; i < trailing; i++) {
            addBlankCell();
        }

        totalText.setText("Bu ay toplam  •  " + formatHours(total) + " saat");
    }

    private void addBlankCell() {
        View blank = new View(this);
        GridLayout.LayoutParams lp = cellParams();
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        blank.setLayoutParams(lp);
        blank.setBackground(rounded(Color.rgb(12, 17, 24), Color.rgb(26, 34, 46), 15, 1));
        calendarGrid.addView(blank);
    }

    private void addDayCell(int day, String key, double hours) {
        boolean today = isToday(day);
        boolean hasHours = hours > 0;

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
        hourText.setText(hasHours ? formatHours(hours) + " sa" : "");
        hourText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hourText.setTextColor(hasHours ? GREEN : TEXT_MUTED);
        hourText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hourText.setGravity(Gravity.CENTER);
        hourText.setPadding(0, dp(6), 0, 0);
        cell.addView(hourText);

        cell.setOnClickListener(v -> showHourDialog(day, key, hours));

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

    private void showHourDialog(int day, String key, double currentHours) {
        String[] months = new DateFormatSymbols(tr).getMonths();
        String dateText = day + " " + capitalize(months[shownMonth.get(Calendar.MONTH)]) + " " + shownMonth.get(Calendar.YEAR);

        EditText input = new EditText(this);
        input.setHint("Örn: 2 veya 2,5");
        input.setHintTextColor(Color.rgb(117, 132, 151));
        input.setTextColor(TEXT);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSelectAllOnFocus(true);
        input.setText(currentHours > 0 ? formatHours(currentHours) : "");
        input.setSingleLine(true);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackground(rounded(SURFACE_ALT, STROKE, 12, 1));

        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(dp(22), dp(4), dp(22), 0);
        holder.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(dateText)
                .setMessage("Mesai saati")
                .setView(holder)
                .setNegativeButton("İptal", null)
                .setNeutralButton("Sil", null)
                .setPositiveButton("Kaydet", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String raw = input.getText().toString().trim().replace(',', '.');
                if (raw.isEmpty()) {
                    prefs.edit().remove(key).apply();
                    dialog.dismiss();
                    refreshCalendar();
                    return;
                }

                try {
                    double value = Double.parseDouble(raw);
                    if (value < 0 || value > 24) {
                        Toast.makeText(this, "0 ile 24 saat arasında bir değer girin.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value == 0) {
                        prefs.edit().remove(key).apply();
                    } else {
                        prefs.edit().putString(key, String.valueOf(value)).apply();
                    }

                    dialog.dismiss();
                    refreshCalendar();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Geçerli bir saat değeri girin.", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                prefs.edit().remove(key).apply();
                dialog.dismiss();
                refreshCalendar();
            });
        });

        dialog.show();
    }

    private void showSettings() {
        new AlertDialog.Builder(this)
                .setTitle("Ayarlar")
                .setMessage("Mesai Takvimi\n\nYapımcı: Ömer Faruk Boz\nSürüm: 1.1")
                .setPositiveButton("Tamam", null)
                .show();
    }

    private void changeMonth(int amount) {
        shownMonth.add(Calendar.MONTH, amount);
        shownMonth.set(Calendar.DAY_OF_MONTH, 1);
        refreshCalendar();
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

    private double getHours(String key) {
        try {
            return Double.parseDouble(prefs.getString(key, "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    private String makeKey(int year, int monthZeroBased, int day) {
        return String.format(tr, "%04d-%02d-%02d", year, monthZeroBased + 1, day);
    }

    private String formatHours(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.00001) {
            return String.valueOf((int) Math.rint(value));
        }
        String text = String.format(tr, "%.2f", value);
        while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
        if (text.endsWith(",")) text = text.substring(0, text.length() - 1);
        return text;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase(tr) + value.substring(1);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class SwipeListener extends GestureDetector.SimpleOnGestureListener {
        private static final int MIN_DISTANCE = 110;
        private static final int MIN_VELOCITY = 120;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;

            float dx = e2.getX() - e1.getX();
            float dy = e2.getY() - e1.getY();

            if (Math.abs(dx) > Math.abs(dy)
                    && Math.abs(dx) > MIN_DISTANCE
                    && Math.abs(velocityX) > MIN_VELOCITY) {
                changeMonth(dx < 0 ? 1 : -1);
                return true;
            }
            return false;
        }
    }
}
