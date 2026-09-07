package com.omerfaruk.mesaitakvimi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private final Locale tr = new Locale("tr", "TR");
    private Calendar shownMonth;
    private SharedPreferences prefs;
    private TextView monthTitle;
    private TextView totalText;
    private GridLayout calendarGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("mesai_kayitlari", MODE_PRIVATE);
        shownMonth = Calendar.getInstance(tr);
        shownMonth.set(Calendar.DAY_OF_MONTH, 1);
        buildScreen();
        refreshCalendar();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(12));
        root.setBackgroundColor(Color.rgb(247, 249, 252));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Mesai Takvimi");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(25, 35, 50));
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button settings = new Button(this);
        settings.setText("Ayarlar");
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> showSettings());
        top.addView(settings);
        root.addView(top);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(0, dp(14), 0, dp(8));

        Button prev = new Button(this);
        prev.setText("‹");
        prev.setTextSize(24);
        prev.setOnClickListener(v -> {
            shownMonth.add(Calendar.MONTH, -1);
            refreshCalendar();
        });
        nav.addView(prev, new LinearLayout.LayoutParams(dp(54), dp(48)));

        monthTitle = new TextView(this);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setTextSize(20);
        monthTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        monthTitle.setTextColor(Color.rgb(30, 40, 55));
        nav.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button next = new Button(this);
        next.setText("›");
        next.setTextSize(24);
        next.setOnClickListener(v -> {
            shownMonth.add(Calendar.MONTH, 1);
            refreshCalendar();
        });
        nav.addView(next, new LinearLayout.LayoutParams(dp(54), dp(48)));
        root.addView(nav);

        totalText = new TextView(this);
        totalText.setGravity(Gravity.CENTER);
        totalText.setTextSize(18);
        totalText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        totalText.setTextColor(Color.rgb(21, 101, 192));
        totalText.setPadding(dp(10), dp(12), dp(10), dp(12));
        totalText.setBackgroundColor(Color.WHITE);
        root.addView(totalText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        calendarGrid = new GridLayout(this);
        calendarGrid.setColumnCount(7);
        calendarGrid.setPadding(0, dp(10), 0, 0);
        scroll.addView(calendarGrid, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void refreshCalendar() {
        calendarGrid.removeAllViews();

        String[] months = new DateFormatSymbols(tr).getMonths();
        String monthName = months[shownMonth.get(Calendar.MONTH)];
        monthTitle.setText(capitalize(monthName) + " " + shownMonth.get(Calendar.YEAR));

        String[] dayNames = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
        for (String dayName : dayNames) {
            TextView day = new TextView(this);
            day.setText(dayName);
            day.setGravity(Gravity.CENTER);
            day.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            day.setTextColor(Color.rgb(90, 100, 115));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(38);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            day.setLayoutParams(lp);
            calendarGrid.addView(day);
        }

        Calendar first = (Calendar) shownMonth.clone();
        int firstDay = first.get(Calendar.DAY_OF_WEEK); // Pazar=1
        int mondayIndex = (firstDay + 5) % 7;

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

        while ((calendarGrid.getChildCount() - 7) % 7 != 0) {
            addBlankCell();
        }

        totalText.setText("Bu ay toplam: " + formatHours(total) + " saat");
    }

    private void addBlankCell() {
        View blank = new View(this);
        GridLayout.LayoutParams lp = cellParams();
        blank.setLayoutParams(lp);
        calendarGrid.addView(blank);
    }

    private void addDayCell(int day, String key, double hours) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(2), dp(5), dp(2), dp(5));
        cell.setBackgroundColor(Color.WHITE);

        TextView dayNo = new TextView(this);
        dayNo.setText(String.valueOf(day));
        dayNo.setTextSize(16);
        dayNo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        dayNo.setTextColor(Color.rgb(35, 45, 60));
        dayNo.setGravity(Gravity.CENTER);
        cell.addView(dayNo);

        TextView hourText = new TextView(this);
        hourText.setText(hours > 0 ? formatHours(hours) + " sa" : "");
        hourText.setTextSize(13);
        hourText.setTextColor(Color.rgb(21, 101, 192));
        hourText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hourText.setGravity(Gravity.CENTER);
        cell.addView(hourText);

        cell.setOnClickListener(v -> showHourDialog(day, key, hours));

        GridLayout.LayoutParams lp = cellParams();
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        cell.setLayoutParams(lp);
        calendarGrid.addView(cell);
    }

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(68);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        return lp;
    }

    private void showHourDialog(int day, String key, double currentHours) {
        String[] months = new DateFormatSymbols(tr).getMonths();
        String dateText = day + " " + capitalize(months[shownMonth.get(Calendar.MONTH)]) + " " + shownMonth.get(Calendar.YEAR);

        EditText input = new EditText(this);
        input.setHint("Örn: 2 veya 2,5");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSelectAllOnFocus(true);
        input.setText(currentHours > 0 ? formatHours(currentHours) : "");
        int pad = dp(20);
        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(pad, 0, pad, 0);
        holder.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(dateText)
                .setMessage("Yapılan mesai süresini saat olarak girin.")
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
                .setMessage("Uygulama: Mesai Takvimi\n\nYapımcı: Ömer Faruk Boz\n\nSürüm: 1.0\n\nMesai kayıtları yalnızca bu cihazda saklanır.")
                .setPositiveButton("Tamam", null)
                .show();
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
}
