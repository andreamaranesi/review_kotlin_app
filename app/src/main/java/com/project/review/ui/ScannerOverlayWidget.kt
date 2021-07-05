package com.project.review.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors
import com.project.review.R

/**
 * creates the transparent rectangle in the center
 * the graphics component is used by ScannerActivity
 *
 * @see ScannerActivity
 */
class ScannerOverlayWidget : View {
    private var mSemiBlackPaint: Paint? = null
    private val mPath: Path = Path()

    companion object {
        const val widthDivider = 9
        const val heightDivider = 2.6
        const val borderSize = 50F
    }

    private lateinit var attrs: AttributeSet

    constructor(context: Context) : super(context) {
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.attrs = attrs
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        this.attrs = attrs
        initPaints()
    }

    private fun initPaints() {
        mSemiBlackPaint = Paint()
        mSemiBlackPaint?.color = Color.TRANSPARENT
        mSemiBlackPaint?.strokeWidth = 200F
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPath.reset()

        val start = (canvas.width / widthDivider).toFloat()
        val top = (canvas.height / heightDivider).toFloat()
        val end =
            (canvas.width / widthDivider + (canvas.width - (canvas.width / widthDivider) * 2)).toFloat()
        val bottom =
            (canvas.height / heightDivider + (canvas.height - (canvas.height / heightDivider) * 2)).toFloat()

        mPath.addRect(
            start,
            top,
            end,
            bottom,
            Path.Direction.CW
        )

        val borderPaint = Paint()
        borderPaint.strokeWidth = 20F
        borderPaint.strokeCap = Paint.Cap.ROUND
        val color = MaterialColors.getColor(rootView, R.attr.colorPrimary)
        borderPaint.color = color

        mPath.fillType = (Path.FillType.INVERSE_EVEN_ODD)

        canvas.drawLine(
            start,
            top,
            start + borderSize,
            top,
            borderPaint
        )
        canvas.drawLine(
            start,
            top,
            start,
            top + borderSize,
            borderPaint
        )
        canvas.drawLine(
            end - borderSize,
            top,
            end,
            top,
            borderPaint
        )

        canvas.drawLine(
            end,
            top,
            end,
            top + borderSize,
            borderPaint
        )
        canvas.drawLine(
            start,
            bottom,
            start + borderSize,
            bottom,
            borderPaint
        )
        canvas.drawLine(
            start,
            bottom - borderSize,
            start,
            bottom,
            borderPaint
        )
        canvas.drawLine(
            end - borderSize,
            bottom,
            end,
            bottom,
            borderPaint
        )
        canvas.drawLine(
            end,
            bottom - borderSize,
            end,
            bottom,
            borderPaint
        )

        canvas.drawPath(mPath, mSemiBlackPaint!!)
        canvas.clipPath(mPath)
        canvas.drawColor(Color.parseColor("#A6000000"))
    }
}