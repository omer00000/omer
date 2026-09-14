package com.omerfaruk.mesaitakvimi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends Activity {

    private static final int BG = Color.parseColor("#000000");
    private static final int SURFACE = Color.parseColor("#0E0E0E");
    private static final int SURFACE_ALT = Color.parseColor("#141414");
    private static final int STROKE = Color.parseColor("#2A2A2A");
    private static final int TEXT = Color.parseColor("#F2F2F2");
    private static final int TEXT_MUTED = Color.parseColor("#A2A2A2");
    private static final int ACCENT = Color.parseColor("#EDEDED");
    private static final int ACCENT_SOFT = Color.parseColor("#1C1C1C");
    private static final int GREEN = Color.parseColor("#F2F2F2");

    private static final String PREFS_NAME = "mesai_kayitlari";
    private static final String PREFIX_MINUTES = "m:";
    private static final String BACKUP_FORMAT = "MesaiTakvimiBackup";
    private static final int BACKUP_VERSION = 1;
    private static final int REQUEST_EXPORT_BACKUP = 1201;
    private static final int REQUEST_IMPORT_BACKUP = 1202;

    private final Locale tr = new Locale("tr", "TR");

    private Calendar shownMonth;
    private SharedPreferences prefs;
    private DotMatrixTextView monthTitle;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        if (requestCode == REQUEST_EXPORT_BACKUP) {
            writeBackup(uri);
        } else if (requestCode == REQUEST_IMPORT_BACKUP) {
            new AlertDialog.Builder(this)
                    .setTitle("Yedeği içe aktar")
                    .setMessage("Mevcut kayıtlar korunur. Yedekte aynı tarih varsa o kayıt yedekteki değerle değiştirilir. Devam edilsin mi?")
                    .setNegativeButton("İptal", null)
                    .setPositiveButton("İçe aktar", (dialog, which) -> importBackup(uri))
                    .show();
        }
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), getStatusBarInset() + dp(14), dp(16), dp(16));
        root.setBackgroundColor(BG);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.TOP | Gravity.CENTER_VERTICAL);
        top.setPadding(0, dp(6), 0, 0);

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        top.addView(titleGroup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        DotMatrixTextView appTitle = new DotMatrixTextView(this);
        appTitle.setText(normalizeDotsText("Mesai Takvimi"));
        appTitle.setDotColor(TEXT);
        appTitle.setMatrixMetrics(dpFloat(3.2f), dpFloat(2.5f), dpFloat(7f));
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
        nav.setPadding(0, dp(24), 0, dp(16));

        TextView prev = navButton("‹");
        prev.setContentDescription("Önceki ay");
        prev.setOnClickListener(v -> animateMonthChange(-1));
        nav.addView(prev);

        monthTitle = new DotMatrixTextView(this);
        monthTitle.setDotColor(TEXT);
        monthTitle.setMatrixMetrics(dpFloat(3.0f), dpFloat(2.3f), dpFloat(6.5f));
        monthTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams monthLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        monthLp.leftMargin = dp(10);
        monthLp.rightMargin = dp(10);
        nav.addView(monthTitle, monthLp);

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
        weekHeader.setPadding(0, dp(18), 0, dp(8));
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
        monthTitle.setText(normalizeDotsText(capitalize(monthName) + " " + shownMonth.get(Calendar.YEAR)));

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
        blank.setBackground(rounded(Color.parseColor("#050505"), Color.parseColor("#171717"), 18, 1));
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
        int border = today ? ACCENT : STROKE;
        int borderWidth = today ? 2 : 1;
        cell.setBackground(rounded(fill, border, 18, borderWidth));
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
        hourText.setTextColor(hasHours ? TEXT : TEXT_MUTED);
        hourText.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
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
        String[] items = {
                "Yedeği dışa aktar",
                "Yedekten içe aktar",
                "Hakkında"
        };

        new AlertDialog.Builder(this)
                .setTitle("Ayarlar")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        startBackupExport();
                    } else if (which == 1) {
                        startBackupImport();
                    } else {
                        showAbout();
                    }
                })
                .setNegativeButton("Kapat", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("Mesai Takvimi")
                .setMessage("Yapımcı: Ömer Faruk Boz\nSürüm: 1.4\n\nVeriler yalnızca telefonda saklanır. Yedekleme ile JSON dosyası olarak dışa ve içe aktarılabilir.")
                .setPositiveButton("Tamam", null)
                .show();
    }

    private void startBackupExport() {
        String date = new SimpleDateFormat("yyyy-MM-dd", tr).format(new Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "Mesai-Takvimi-Yedek-" + date + ".json");
        startActivityForResult(intent, REQUEST_EXPORT_BACKUP);
    }

    private void startBackupImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/json",
                "text/plain",
                "application/octet-stream"
        });
        startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
    }

    private void writeBackup(Uri uri) {
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) {
                throw new IllegalStateException("Dosya açılamadı");
            }

            String json = buildBackupJson().toString(2);
            output.write(json.getBytes(StandardCharsets.UTF_8));
            output.flush();

            Toast.makeText(this, "Yedek başarıyla dışa aktarıldı.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Dışa aktarma başarısız")
                    .setMessage("Yedek dosyası yazılamadı.\n\n" + safeError(e))
                    .setPositiveButton("Tamam", null)
                    .show();
        }
    }

    private JSONObject buildBackupJson() throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", BACKUP_FORMAT);
        root.put("version", BACKUP_VERSION);
        root.put("app", "Mesai Takvimi");
        root.put("exportedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(new Date()));

        JSONObject records = new JSONObject();
        TreeMap<String, Integer> sorted = new TreeMap<>();

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (!isDateKey(key)) continue;

            int minutes = getStoredMinutes(key);
            if (minutes > 0) {
                sorted.put(key, minutes);
            }
        }

        for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
            records.put(entry.getKey(), entry.getValue());
        }

        root.put("recordCount", sorted.size());
        root.put("records", records);
        return root;
    }

    private void importBackup(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IllegalStateException("Dosya açılamadı");
            }

            String jsonText = readUtf8(input);
            JSONObject root = new JSONObject(jsonText);

            if (!BACKUP_FORMAT.equals(root.optString("format"))) {
                throw new IllegalArgumentException("Bu dosya Mesai Takvimi yedeği değil.");
            }

            int version = root.optInt("version", -1);
            if (version < 1 || version > BACKUP_VERSION) {
                throw new IllegalArgumentException("Desteklenmeyen yedek sürümü: " + version);
            }

            JSONObject records = root.optJSONObject("records");
            if (records == null) {
                throw new IllegalArgumentException("Yedekte kayıt bölümü bulunamadı.");
            }

            SharedPreferences.Editor editor = prefs.edit();
            Iterator<String> keys = records.keys();
            int imported = 0;
            int skipped = 0;

            while (keys.hasNext()) {
                String key = keys.next();
                if (!isDateKey(key)) {
                    skipped++;
                    continue;
                }

                int minutes = records.optInt(key, -1);
                if (minutes < 0 || minutes > 24 * 60) {
                    skipped++;
                    continue;
                }

                if (minutes == 0) {
                    editor.remove(key);
                } else {
                    editor.putString(key, PREFIX_MINUTES + minutes);
                }
                imported++;
            }

            editor.apply();
            refreshCalendar();

            String message = imported + " kayıt içe aktarıldı.";
            if (skipped > 0) {
                message += "\n" + skipped + " geçersiz kayıt atlandı.";
            }

            new AlertDialog.Builder(this)
                    .setTitle("İçe aktarma tamamlandı")
                    .setMessage(message)
                    .setPositiveButton("Tamam", null)
                    .show();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("İçe aktarma başarısız")
                    .setMessage("Yedek dosyası okunamadı.\n\n" + safeError(e))
                    .setPositiveButton("Tamam", null)
                    .show();
        }
    }

    private String readUtf8(InputStream input) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private boolean isDateKey(String key) {
        return key != null && key.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    private String safeError(Exception e) {
        String message = e.getMessage();
        return TextUtils.isEmpty(message) ? e.getClass().getSimpleName() : message;
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

    private String normalizeDotsText(String value) {
        if (value == null) return "";
        String upper = value.toUpperCase(tr);
        return upper.replace('Ç', 'C')
                .replace('Ğ', 'G')
                .replace('İ', 'I')
                .replace('Ö', 'O')
                .replace('Ş', 'S')
                .replace('Ü', 'U');
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase(tr) + value.substring(1);
    }

    private int getStatusBarInset() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return getResources().getDimensionPixelSize(resId);
        }
        return dp(24);
    }

    private float dpFloat(float value) {
        return value * getResources().getDisplayMetrics().density;
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

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)
                    && Math.abs(diffX) > MIN_DISTANCE
                    && Math.abs(velocityX) > MIN_VELOCITY) {
                if (diffX < 0) {
                    animateMonthChange(1);
                } else {
                    animateMonthChange(-1);
                }
                return true;
            }
            return false;
        }
    }
}
