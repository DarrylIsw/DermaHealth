package com.example.dermahealth.ui

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.dermahealth.R
import kotlin.math.roundToInt

class SwipeActionsCallback(
    context: Context,
    private val onSwipedLeft: (position: Int) -> Unit,    // DELETE
    private val onSwipedRight: (position: Int) -> Unit    // EDIT
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val red = ContextCompat.getColor(context, R.color.deleteSwipeBackground)     // e.g. #D32F2F
    private val white = ContextCompat.getColor(context, R.color.deleteSwipeForeground)   // e.g. #FFFFFF
    private val blue = ContextCompat.getColor(context, R.color.editSwipeBackground)      // e.g. #1976D2

    private val paintDeleteBg = Paint().apply { color = red }
    private val paintEditBg = Paint().apply { color = blue }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        textSize = sp(context, 14f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val iconDelete = AppCompatResources.getDrawable(context, R.drawable.ic_delete)?.toBitmap()
    private val iconEdit = AppCompatResources.getDrawable(context, R.drawable.ic_edit)?.toBitmap()
    private val corner = dp(context, 8f)
    private val pad = dp(context, 16f)
    private val iconSize = dp(context, 24f).roundToInt()
    private val gap = dp(context, 8f)

    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

    override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
        val pos = vh.bindingAdapterPosition
        when (direction) {
            ItemTouchHelper.LEFT -> onSwipedLeft(pos)
            ItemTouchHelper.RIGHT -> onSwipedRight(pos)
        }
    }

    override fun getSwipeThreshold(vh: RecyclerView.ViewHolder): Float = 0.35f

    override fun onChildDraw(
        c: Canvas,
        rv: RecyclerView,
        vh: RecyclerView.ViewHolder,
        dX: Float, dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val item = vh.itemView
        val h = (item.bottom - item.top).toFloat()
        val centerY = item.top + h / 2f

        if (dX > 0f) {
            // RIGHT = EDIT (blue)
            val rect = RectF(item.left.toFloat(), item.top.toFloat(), (item.left + dX).coerceAtMost(item.right.toFloat()), item.bottom.toFloat())
            c.drawRoundRect(rect, corner, corner, paintEditBg)

            val iconTop = centerY - iconSize / 2f
            val iconLeft = item.left + pad
            iconEdit?.let { bmp ->
                val scaled = Bitmap.createScaledBitmap(bmp, iconSize, iconSize, true)
                c.drawBitmap(scaled, iconLeft, iconTop, null)
            }
            val text = rv.context.getString(R.string.edit_notes) // “Edit Notes”
            val textX = iconLeft + iconSize + gap
            val textY = centerY + textCenterOffset(text)
            c.drawText(text, textX, textY, paintText)
        } else if (dX < 0f) {
            // LEFT = DELETE (red)
            val rect = RectF((item.right + dX).coerceAtLeast(item.left.toFloat()), item.top.toFloat(), item.right.toFloat(), item.bottom.toFloat())
            c.drawRoundRect(rect, corner, corner, paintDeleteBg)

            val iconTop = centerY - iconSize / 2f
            val iconRight = item.right - pad
            iconDelete?.let { bmp ->
                val scaled = Bitmap.createScaledBitmap(bmp, iconSize, iconSize, true)
                c.drawBitmap(scaled, iconRight - iconSize, iconTop, null)
            }
            val text = rv.context.getString(R.string.delete)
            val textWidth = paintText.measureText(text)
            val textX = iconRight - iconSize - gap - textWidth
            val textY = centerY + textCenterOffset(text)
            c.drawText(text, textX, textY, paintText)
        }

        super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
    }

    private fun textCenterOffset(text: String): Float {
        val bounds = Rect()
        paintText.getTextBounds(text, 0, text.length, bounds)
        return bounds.height() / 2f - 3f
    }

    private fun dp(ctx: Context, v: Float) = v * ctx.resources.displayMetrics.density
    private fun sp(ctx: Context, v: Float) = v * ctx.resources.displayMetrics.scaledDensity
}
