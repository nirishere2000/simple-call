package com.nirotem.simplecall.ui.heartShape

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class HeartImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val heartPath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        createHeartPath(w, h)
    }

    private fun createHeartPath(width: Int, height: Int) {
        heartPath.reset()

        // Move to a point slightly below the top center (the "indentation" at the top)
        heartPath.moveTo(width * 0.5f, height * 0.2f)

        // === Left side curve ===
        // Control point #1 (width * 0.1f, height * 0.0f): pulls the curve left & upward
        // Control point #2 (width * 0.0f, height * 0.5f): pulls the curve inward/down
        // End at bottom tip (width * 0.5f, height * 0.85f): the bottom point
        heartPath.cubicTo(
            width * 0.1f, height * 0.0f,
            width * 0.0f, height * 0.5f,
            width * 0.5f, height * 0.85f
        )

        // === Right side curve ===
        // Mirrors the left side:
        // Control point #1 (width * 1.0f, height * 0.5f) pulls inward/down from the right
        // Control point #2 (width * 0.9f, height * 0.0f) pulls upward/right
        // End back at top indentation (width * 0.5f, height * 0.2f)
        heartPath.cubicTo(
            width * 1.0f, height * 0.5f,
            width * 0.9f, height * 0.0f,
            width * 0.5f, height * 0.2f
        )

        heartPath.close()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        // Clip the canvas to our heart-shaped path
        canvas.clipPath(heartPath)
        // Draw the image normally (it will be clipped by the heart)
        super.onDraw(canvas)
        canvas.restore()
    }
}
