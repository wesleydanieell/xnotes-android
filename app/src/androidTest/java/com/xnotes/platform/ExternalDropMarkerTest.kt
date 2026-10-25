package com.xnotes.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.test.InstrumentationTestCase
import com.xnotes.core.geometry.Pt

/** The external-drop marker follows the currently selected app accent. */
@Suppress("DEPRECATION")
class ExternalDropMarkerTest : InstrumentationTestCase() {
    fun testMarkerUsesCurrentAccentOnEveryDraw() {
        var accent = Color.RED
        val marker = ExternalDrop.DropMarker(1f) { accent }
        marker.at = Pt(32.0, 32.0)
        marker.setBounds(0, 0, 64, 64)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            marker.draw(canvas)
            assertEquals(Color.RED, bitmap.getPixel(32, 32))
            accent = Color.GREEN
            bitmap.eraseColor(Color.TRANSPARENT)
            marker.draw(canvas)
            assertEquals(Color.GREEN, bitmap.getPixel(32, 32))
        } finally {
            bitmap.recycle()
        }
    }
}
