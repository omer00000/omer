from pathlib import Path
import re

path = Path("app/src/main/java/com/omerfaruk/mesaitakvimi/MainActivity.java")
s = path.read_text(encoding="utf-8")
original = s

# Nothing OS benzeri tek renkli palet.
s = re.sub(
    r'    private static final int BG = .*?\n    private static final int GREEN = .*?;\n',
    '''    private static final int BG = Color.parseColor("#000000");
    private static final int SURFACE = Color.parseColor("#0E0E0E");
    private static final int SURFACE_ALT = Color.parseColor("#141414");
    private static final int STROKE = Color.parseColor("#2A2A2A");
    private static final int TEXT = Color.parseColor("#F2F2F2");
    private static final int TEXT_MUTED = Color.parseColor("#A2A2A2");
    private static final int ACCENT = Color.parseColor("#EDEDED");
    private static final int ACCENT_SOFT = Color.parseColor("#1C1C1C");
    private static final int GREEN = Color.parseColor("#F2F2F2");
''',
    s,
    count=1,
    flags=re.S,
)

# Başlığı ve ay başlığını dot-matrix bileşenine çevir.
s = s.replace("    private TextView monthTitle;", "    private DotMatrixTextView monthTitle;")

s = re.sub(
    r'        TextView appTitle = new TextView\(this\);.*?        titleGroup\.addView\(appTitle\);',
    '''        DotMatrixTextView appTitle = new DotMatrixTextView(this);
        appTitle.setText(normalizeDotsText("Mesai Takvimi"));
        appTitle.setDotColor(TEXT);
        appTitle.setMatrixMetrics(dpFloat(3.2f), dpFloat(2.5f), dpFloat(7f));
        titleGroup.addView(appTitle);''',
    s,
    count=1,
    flags=re.S,
)

s = re.sub(
    r'        monthTitle = new TextView\(this\);.*?        nav\.addView\(monthTitle, new LinearLayout\.LayoutParams\(0, dp\(46\), 1f\)\);',
    '''        monthTitle = new DotMatrixTextView(this);
        monthTitle.setDotColor(TEXT);
        monthTitle.setMatrixMetrics(dpFloat(3.0f), dpFloat(2.3f), dpFloat(6.5f));
        monthTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams monthLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        monthLp.leftMargin = dp(10);
        monthLp.rightMargin = dp(10);
        nav.addView(monthTitle, monthLp);''',
    s,
    count=1,
    flags=re.S,
)

# İçeriği durum çubuğundan biraz aşağı indir ve boşluğu Nothing arayüzüne yaklaştır.
s = s.replace(
    "root.setPadding(dp(14), dp(18), dp(14), dp(12));",
    "root.setPadding(dp(16), getStatusBarInset() + dp(14), dp(16), dp(16));",
)
s = s.replace(
    "top.setGravity(Gravity.CENTER_VERTICAL);",
    "top.setGravity(Gravity.TOP | Gravity.CENTER_VERTICAL);\n        top.setPadding(0, dp(6), 0, 0);",
)
s = s.replace("nav.setPadding(0, dp(16), 0, dp(12));", "nav.setPadding(0, dp(24), 0, dp(16));")
s = s.replace("weekHeader.setPadding(0, dp(14), 0, dp(5));", "weekHeader.setPadding(0, dp(18), 0, dp(8));")

# Takvim hücrelerini tek renkli hale getir.
s = s.replace(
    'blank.setBackground(rounded(Color.rgb(12, 17, 24), Color.rgb(26, 34, 46), 15, 1));',
    'blank.setBackground(rounded(Color.parseColor("#050505"), Color.parseColor("#171717"), 18, 1));',
)
s = s.replace(
    "int border = today ? ACCENT : (hasHours ? Color.rgb(54, 93, 137) : STROKE);",
    "int border = today ? ACCENT : STROKE;",
)
s = s.replace("cell.setBackground(rounded(fill, border, 15, borderWidth));", "cell.setBackground(rounded(fill, border, 18, borderWidth));")
s = s.replace("hourText.setTextColor(hasHours ? GREEN : TEXT_MUTED);", "hourText.setTextColor(hasHours ? TEXT : TEXT_MUTED);")
s = s.replace("hourText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);", "hourText.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);")

# Dot-matrix ay başlığı için metni normalize et.
s = s.replace(
    'monthTitle.setText(capitalize(monthName) + " " + shownMonth.get(Calendar.YEAR));',
    'monthTitle.setText(normalizeDotsText(capitalize(monthName) + " " + shownMonth.get(Calendar.YEAR)));',
)

# Hakkında sürümü.
s = s.replace("Sürüm: 1.3", "Sürüm: 1.4")

# Durum çubuğu boşluğu ve dpFloat yardımcıları yoksa ekle.
if "private int getStatusBarInset()" not in s:
    marker = "    private int dp(int value) {"
    helper = '''    private int getStatusBarInset() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return getResources().getDimensionPixelSize(resId);
        }
        return dp(24);
    }

    private float dpFloat(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

'''
    s = s.replace(marker, helper + marker, 1)

# normalizeDotsText zaten yeni sürümde yoksa ekle.
if "private String normalizeDotsText(String value)" not in s:
    marker = "    private String capitalize(String value) {"
    normalize = '''    private String normalizeDotsText(String value) {
        if (value == null) return "";
        String upper = value.toUpperCase(tr);
        return upper.replace('Ç', 'C')
                .replace('Ğ', 'G')
                .replace('İ', 'I')
                .replace('Ö', 'O')
                .replace('Ş', 'S')
                .replace('Ü', 'U');
    }

'''
    s = s.replace(marker, normalize + marker, 1)

if s == original:
    raise SystemExit("Nothing stilinde uygulanacak değişiklik bulunamadı.")

path.write_text(s, encoding="utf-8")
print("Nothing tarzı Mesai Takvimi arayüzü uygulandı.")
