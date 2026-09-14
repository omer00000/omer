package com.omerfaruk.mesaitakvimi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class DotMatrixTextView extends View {

    private static final Map<Character, String[]> GLYPHS = new HashMap<>();

    static {
        GLYPHS.put('A', new String[]{"01110","10001","10001","11111","10001","10001","10001"});
        GLYPHS.put('B', new String[]{"11110","10001","10001","11110","10001","10001","11110"});
        GLYPHS.put('C', new String[]{"01111","10000","10000","10000","10000","10000","01111"});
        GLYPHS.put('D', new String[]{"11110","10001","10001","10001","10001","10001","11110"});
        GLYPHS.put('E', new String[]{"11111","10000","10000","11110","10000","10000","11111"});
        GLYPHS.put('F', new String[]{"11111","10000","10000","11110","10000","10000","10000"});
        GLYPHS.put('G', new String[]{"01111","10000","10000","10111","10001","10001","01110"});
        GLYPHS.put('H', new String[]{"10001","10001","10001","11111","10001","10001","10001"});
        GLYPHS.put('I', new String[]{"11111","00100","00100","00100","00100","00100","11111"});
        GLYPHS.put('J', new String[]{"00001","00001","00001","00001","10001","10001","01110"});
        GLYPHS.put('K', new String[]{"10001","10010","10100","11000","10100","10010","10001"});
        GLYPHS.put('L', new String[]{"10000","10000","10000","10000","10000","10000","11111"});
        GLYPHS.put('M', new String[]{"10001","11011","10101","10101","10001","10001","10001"});
        GLYPHS.put('N', new String[]{"10001","11001","10101","10011","10001","10001","10001"});
        GLYPHS.put('O', new String[]{"01110","10001","10001","10001","10001","10001","01110"});
        GLYPHS.put('P', new String[]{"11110","10001","10001","11110","10000","10000","10000"});
        GLYPHS.put('Q', new String[]{"01110","10001","10001","10001","10101","10010","01101"});
        GLYPHS.put('R', new String[]{"11110","10001","10001","11110","10100","10010","10001"});
        GLYPHS.put('S', new String[]{"01111","10000","10000","01110","00001","00001","11110"});
        GLYPHS.put('T', new String[]{"11111","00100","00100","00100","00100","00100","00100"});
        GLYPHS.put('U', new String[]{"10001","10001","10001","10001","10001","10001","01110"});
        GLYPHS.put('V', new String[]{"10001","10001","10001","10001","10001","01010","00100"});
        GLYPHS.put('W', new String[]{"10001","10001","10001","10101","10101","10101","01010"});
        GLYPHS.put('X', new String[]{"10001","10001","01010","00100","01010","10001","10001"});
        GLYPHS.put('Y', new String[]{"10001","10001","01010","00100","00100","00100","00100"});
        GLYPHS.put('Z', new String[]{"11111","00001","00010","00100","01000","10000","11111"});

        GLYPHS.put('0', new String[]{"01110","10001","10011","10101","11001","10001","01110"});
        GLYPHS.put('1', new String[]{"00100","01100","00100","00100","00100","00100","01110"});
        GLYPHS.put('2', new String[]{"01110","10001","00001","00010","00100","01000","11111"});
        GLYPHS.put('3', new String[]{"11110","00001","00001","01110","00001","00001","11110"});
        GLYPHS.put('4', new String[]{"00010","00110","01010","10010","11111","00010","00010"});
        GLYPHS.put('5', new String[]{"11111","10000","10000","11110","00001","00001","11110"});
        GLYPHS.put('6', new String[]{"01110","10000","10000","11110","10001","10001","01110"});
        GLYPHS.put('7', new String[]{"11111","00001","00010","00100","01000","01000","01000"});
        GLYPHS.put('8', new String[]{"01110","10001","10001","01110","10001","10001","01110"});
        GLYPHS.put('9', new String[]{"01110","10001","10001","01111","00001","00001","01110"});

        GLYPHS.put('-', new String[]{"00000","00000","00000","11111","00000","00000","00000"});
        GLYPHS.put('.', new String[]{"00000","00000","00000","00000","00000","00110","00110"});
        GLYPHS.put(':', new String[]{"00000","00110","00110","00000","00110","00110","00000"});
        GLYPHS.put('/', new String[]{"00001","00010","00100","00100","01000","10000","00000"});
        GLYPHS.put(' ', new String[]{"000","000","000","000","000","000","000"});
    }

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String text = "";
    private int dotColor = Color.WHITE;
    private float dotDiameterPx;
    private float dotGapPx;
    private float charGapPx;
    private int gravity = Gravity.START;

    public DotMatrixTextView(Context context) {
        super(context);
        init();
    }

    public DotMatrixTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DotMatrixTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(dotColor);
        setMatrixMetrics(dp(3.2f), dp(2.4f), dp(7f));
    }

    public void setText(String value) {
        text = value == null ? "" : value;
        requestLayout();
        invalidate();
    }

    public void setDotColor(int color) {
        dotColor = color;
        dotPaint.setColor(color);
        invalidate();
    }

    public void setMatrixMetrics(float dotDiameterPx, float dotGapPx, float charGapPx) {
        this.dotDiameterPx = dotDiameterPx;
        this.dotGapPx = dotGapPx;
        this.charGapPx = charGapPx;
        requestLayout();
        invalidate();
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = Math.round(getContentWidth()) + getPaddingLeft() + getPaddingRight();
        int desiredHeight = Math.round(getGlyphHeight()) + getPaddingTop() + getPaddingBottom();
        int measuredWidth = resolveSize(desiredWidth, widthMeasureSpec);
        int measuredHeight = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (TextUtils.isEmpty(text)) return;

        float startX = getPaddingLeft();
        float contentWidth = getContentWidth();
        float available = getWidth() - getPaddingLeft() - getPaddingRight();
        if ((gravity & Gravity.CENTER_HORIZONTAL) == Gravity.CENTER_HORIZONTAL) {
            startX = getPaddingLeft() + Math.max(0, (available - contentWidth) / 2f);
        } else if ((gravity & Gravity.END) == Gravity.END) {
            startX = getWidth() - getPaddingRight() - contentWidth;
        }

        float startY = getPaddingTop();
        float radius = dotDiameterPx / 2f;
        float x = startX;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String[] glyph = getGlyph(c);
            int cols = glyph[0].length();

            for (int row = 0; row < glyph.length; row++) {
                for (int col = 0; col < cols; col++) {
                    if (glyph[row].charAt(col) == '1') {
                        float cx = x + col * (dotDiameterPx + dotGapPx) + radius;
                        float cy = startY + row * (dotDiameterPx + dotGapPx) + radius;
                        canvas.drawCircle(cx, cy, radius, dotPaint);
                    }
                }
            }

            x += getGlyphWidth(c) + charGapPx;
        }
    }

    private String[] getGlyph(char c) {
        String[] glyph = GLYPHS.get(Character.toUpperCase(c));
        if (glyph == null) glyph = GLYPHS.get(' ');
        return glyph;
    }

    private float getContentWidth() {
        if (TextUtils.isEmpty(text)) return 0;
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += getGlyphWidth(text.charAt(i));
            if (i < text.length() - 1) width += charGapPx;
        }
        return width;
    }

    private float getGlyphWidth(char c) {
        String[] glyph = getGlyph(c);
        int cols = glyph[0].length();
        return cols * dotDiameterPx + Math.max(0, cols - 1) * dotGapPx;
    }

    private float getGlyphHeight() {
        return 7 * dotDiameterPx + 6 * dotGapPx;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
