package com.barath.modelviewer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

data class ProjectedLabelItem(
    val nodeName: String,
    val labelText: String,
    var screenX: Float = 0f,
    var screenY: Float = 0f,
    var isVisible: Boolean = false
)

class LabelOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val labels = mutableListOf<ProjectedLabelItem>()
    var areLabelsVisible: Boolean = false
        set(value) {
            field = value
            setWillNotDraw(!value)
            invalidate()
        }

    // Density helpers
    private val density = context.resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    // Pre-allocated drawing tools (ZERO allocation during onDraw)
    private val anchorDotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8") // Cyan-400
        style = Paint.Style.FILL
    }

    private val anchorDotHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4D38BDF8") // Cyan-400 30% alpha
        style = Paint.Style.FILL
    }

    private val anchorDotCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8") // Slate-400
        strokeWidth = dp(1.5f)
        style = Paint.Style.STROKE
    }

    private val badgeBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E60F172A") // Deep slate 90% opacity
        style = Paint.Style.FILL
    }

    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8") // Cyan accent
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F8FAFC")
        textSize = dp(11f)
        isFakeBoldText = true
    }

    private val textBounds = Rect()
    private val badgeRect = RectF()
    private val linePath = Path()

    private val badgePaddingH = dp(7f)
    private val badgePaddingV = dp(4f)
    private val badgeRadius = dp(6f)
    private val anchorRadius = dp(3.5f)
    private val haloRadius = dp(6.5f)
    private val lineOffsetH = dp(24f)
    private val lineOffsetV = dp(16f)

    init {
        setWillNotDraw(true) // Initially hidden until enabled
    }

    fun updateLabels(newLabels: List<ProjectedLabelItem>) {
        labels.clear()
        labels.addAll(newLabels)
        if (areLabelsVisible) {
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!areLabelsVisible || labels.isEmpty()) return

        val viewW = width.toFloat()
        val viewH = height.toFloat()

        for (item in labels) {
            if (!item.isVisible) continue

            val ax = item.screenX
            val ay = item.screenY

            // Don't draw if anchor is completely outside viewport with margin
            if (ax < -dp(50f) || ax > viewW + dp(50f) || ay < -dp(50f) || ay > viewH + dp(50f)) {
                continue
            }

            // Measure text
            textPaint.getTextBounds(item.labelText, 0, item.labelText.length, textBounds)
            val textW = textPaint.measureText(item.labelText)
            val textH = textBounds.height().toFloat()

            val badgeW = textW + badgePaddingH * 2f
            val badgeH = textH + badgePaddingV * 2f

            // Smart badge positioning (keep within bounds)
            val placeRight = (ax + lineOffsetH + badgeW) <= (viewW - dp(8f))
            val placeBottom = (ay + lineOffsetV + badgeH) <= (viewH - dp(8f))

            val targetX = if (placeRight) ax + lineOffsetH else (ax - lineOffsetH - badgeW)
            val targetY = if (placeBottom) ay + lineOffsetV else (ay - lineOffsetV - badgeH)

            // Clamp badge position inside container bounds
            val clampedX = targetX.coerceIn(dp(6f), (viewW - badgeW - dp(6f)).coerceAtLeast(dp(6f)))
            val clampedY = targetY.coerceIn(dp(6f), (viewH - badgeH - dp(6f)).coerceAtLeast(dp(6f)))

            badgeRect.set(
                clampedX,
                clampedY,
                clampedX + badgeW,
                clampedY + badgeH
            )

            // Calculate connector line start and end
            val lineEndX = if (clampedX > ax) clampedX else (clampedX + badgeW)
            val lineEndY = clampedY + badgeH / 2f

            // Draw connector line
            linePath.reset()
            linePath.moveTo(ax, ay)
            linePath.lineTo(lineEndX, lineEndY)
            canvas.drawPath(linePath, linePaint)

            // Draw anchor dot with glowing halo
            canvas.drawCircle(ax, ay, haloRadius, anchorDotHaloPaint)
            canvas.drawCircle(ax, ay, anchorRadius, anchorDotFillPaint)
            canvas.drawCircle(ax, ay, anchorRadius * 0.45f, anchorDotCorePaint)

            // Draw badge background pill & border
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgeBackgroundPaint)
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgeBorderPaint)

            // Draw text
            val textX = clampedX + badgePaddingH
            val textY = clampedY + badgePaddingV + textH - dp(1f)
            canvas.drawText(item.labelText, textX, textY, textPaint)
        }
    }
}
