package com.example.pantrytracker.ui.list

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.pantrytracker.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SwipeToConsumeCallback(
    private val context: Context,
    private val onSwipedAction: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(
    0,
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
) {

    private val checkIcon = ContextCompat.getDrawable(context, R.drawable.ic_check)
    private val greenBg = GradientDrawable().apply {
        setColor(Color.parseColor("#2E7D32"))
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14f * context.resources.displayMetrics.scaledDensity
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var hasVibratedForCurrentSwipe = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        hasVibratedForCurrentSwipe = false
        onSwipedAction(viewHolder.bindingAdapterPosition)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val density = context.resources.displayMetrics.density
        val marginH = (12 * density).toInt()
        val marginV = (4 * density).toInt()
        val cornerRadius = 12 * density
        greenBg.cornerRadius = cornerRadius

        // Trigger subtle haptic click when swipe crosses 30% of view width
        val threshold = itemView.width * 0.3f
        if (isCurrentlyActive && abs(dX) >= threshold && !hasVibratedForCurrentSwipe) {
            hasVibratedForCurrentSwipe = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                itemView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                itemView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
        } else if (abs(dX) < threshold) {
            hasVibratedForCurrentSwipe = false
        }

        if (dX < 0) {
            // Swiping Left
            val bgLeft = max(itemView.left + marginH, (itemView.right + dX).toInt())
            val bgRight = itemView.right - marginH
            val bgTop = itemView.top + marginV
            val bgBottom = itemView.bottom - marginV

            if (bgLeft < bgRight) {
                greenBg.setBounds(bgLeft, bgTop, bgRight, bgBottom)
                greenBg.draw(c)

                checkIcon?.let { icon ->
                    val iconSize = (24 * density).toInt()
                    val iconMargin = (16 * density).toInt()
                    val iconTop = bgTop + (bgBottom - bgTop - iconSize) / 2
                    val iconBottom = iconTop + iconSize
                    val iconRight = bgRight - iconMargin
                    val iconLeft = iconRight - iconSize

                    if (bgLeft <= iconLeft) {
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)

                        // Draw "Consumed" text if there is sufficient space
                        val text = "Consumed"
                        val textWidth = textPaint.measureText(text)
                        val textMargin = (8 * density).toInt()
                        val textRight = iconLeft - textMargin
                        val textLeft = textRight - textWidth

                        if (bgLeft <= textLeft) {
                            val textBaseline = bgTop + (bgBottom - bgTop) / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                            c.drawText(text, textLeft, textBaseline, textPaint)
                        }
                    }
                }
            }
        } else if (dX > 0) {
            // Swiping Right
            val bgLeft = itemView.left + marginH
            val bgRight = min(itemView.right - marginH, (itemView.left + dX).toInt())
            val bgTop = itemView.top + marginV
            val bgBottom = itemView.bottom - marginV

            if (bgLeft < bgRight) {
                greenBg.setBounds(bgLeft, bgTop, bgRight, bgBottom)
                greenBg.draw(c)

                checkIcon?.let { icon ->
                    val iconSize = (24 * density).toInt()
                    val iconMargin = (16 * density).toInt()
                    val iconTop = bgTop + (bgBottom - bgTop - iconSize) / 2
                    val iconBottom = iconTop + iconSize
                    val iconLeft = bgLeft + iconMargin
                    val iconRight = iconLeft + iconSize

                    if (bgRight >= iconRight) {
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)

                        // Draw "Consumed" text if there is sufficient space
                        val text = "Consumed"
                        val textMargin = (8 * density).toInt()
                        val textLeft = (iconRight + textMargin).toFloat()
                        val textWidth = textPaint.measureText(text)

                        if (bgRight >= textLeft + textWidth) {
                            val textBaseline = bgTop + (bgBottom - bgTop) / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                            c.drawText(text, textLeft, textBaseline, textPaint)
                        }
                    }
                }
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}