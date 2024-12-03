package com.example.shickjip

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.TypefaceSpan

class CustomTypefaceSpan(private val customTypeface: Typeface) : TypefaceSpan("") {
    override fun updateDrawState(textPaint: TextPaint) {
        applyCustomTypeFace(textPaint, customTypeface)
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        applyCustomTypeFace(textPaint, customTypeface)
    }

    private fun applyCustomTypeFace(paint: Paint, typeface: Typeface) {
        val oldStyle = paint.typeface?.style ?: 0
        val fake = oldStyle and typeface.style.inv()

        if (fake and Typeface.BOLD != 0) {
            paint.isFakeBoldText = true
        }

        if (fake and Typeface.ITALIC != 0) {
            paint.textSkewX = -0.25f
        }

        paint.typeface = typeface
    }
}