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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SwipeActionsCallback(
    context: Context,
    private val onRequestLeft: (position: Int, done: () -> Unit) -> Unit,   // DELETE
    private val onRequestRight: (position: Int, done: () -> Unit) -> Unit,  // EDIT
    private val swipeThreshold: Float = 0.35f
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

    private var isHandling = false

    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 1f
    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 10f
    override fun getSwipeVelocityThreshold(defaultValue: Float): Float = defaultValue * 10f
    override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) { /* no-op (we handle manually) */ }


    override fun onChildDraw(
        c: Canvas,
        rv: RecyclerView,
        vh: RecyclerView.ViewHolder,
        dX: Float, dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val item = vh.itemView
        // Check if inner RecyclerView is scrolling
        val innerRv = item.findViewById<RecyclerView>(R.id.rvImages)
        if (innerRv != null && innerRv.isDragging()) {
            // if inner RV is being dragged, cancel parent swipe
            super.onChildDraw(c, rv, vh, 0f, dY, actionState, isCurrentlyActive)
            return
        }
        val w = max(item.width, 1)
        val lockRatio = 0.50f      // max reveal distance (30% of row width)
        val triggerRatio = swipeThreshold   // trigger threshold (15% of row width)

        // 1) Clamp how far the card can travel
        val maxReveal = w * lockRatio
        val drawDx = min(max(dX, -maxReveal), maxReveal)

        // 2) Draw your backgrounds using the CLAMPED distance
        val h = (item.bottom - item.top).toFloat()
        val centerY = item.top + h / 2f

        if (drawDx > 0f) {
            val rect = RectF(item.left.toFloat(), item.top.toFloat(), item.left + drawDx, item.bottom.toFloat())
            c.drawRoundRect(rect, corner, corner, paintEditBg)
            val iconTop = centerY - iconSize / 2f
            val iconLeft = item.left + pad
            iconEdit?.let { bmp ->
                val scaled = android.graphics.Bitmap.createScaledBitmap(bmp, iconSize, iconSize, true)
                c.drawBitmap(scaled, iconLeft, iconTop, null)
            }
            val t = rv.context.getString(R.string.edit_notes)
            c.drawText(t, iconLeft + iconSize + gap, centerY + textCenterOffset(t), paintText)
        } else if (drawDx < 0f) {
            val rect = RectF(item.right + drawDx, item.top.toFloat(), item.right.toFloat(), item.bottom.toFloat())
            c.drawRoundRect(rect, corner, corner, paintDeleteBg)
            val iconTop = centerY - iconSize / 2f
            val iconRight = item.right - pad
            iconDelete?.let { bmp ->
                val scaled = android.graphics.Bitmap.createScaledBitmap(bmp, iconSize, iconSize, true)
                c.drawBitmap(scaled, iconRight - iconSize, iconTop, null)
            }
            val t = rv.context.getString(R.string.delete)
            val tW = paintText.measureText(t)
            c.drawText(t, iconRight - iconSize - gap - tW, centerY + textCenterOffset(t), paintText)
        }

        // translate with CLAMPED distance
        super.onChildDraw(c, rv, vh, drawDx, dY, actionState, isCurrentlyActive)

        // finger up → decide once; if already handling, bail
        if (!isCurrentlyActive && !isHandling) {
            val crossed = abs(dX) >= (item.width * swipeThreshold)
            if (crossed) {
                isHandling = true
                val pos = vh.bindingAdapterPosition
                item.animate()
                    .translationX(0f)
                    .setDuration(120L)
                    .withEndAction {
                        rv.post { rv.adapter?.notifyItemChanged(pos) }
                        val done: () -> Unit = {
                            isHandling = false
                            rv.post { rv.adapter?.notifyItemChanged(pos) }
                        }
                        if (dX > 0) onRequestRight(pos, done)
                        else onRequestLeft(pos, done)  // <-- LEFT = DELETE
                    }
                    .start()
            } else {
                item.animate().translationX(0f).setDuration(120L).start()
            }
        }
    }

    private fun textCenterOffset(text: String): Float {
        val b = android.graphics.Rect()
        paintText.getTextBounds(text, 0, text.length, b)
        return b.height() / 2f - 3f
    }
    fun RecyclerView.isDragging(): Boolean {
        return this.scrollState == RecyclerView.SCROLL_STATE_DRAGGING
    }

    private fun dp(ctx: Context, v: Float) = v * ctx.resources.displayMetrics.density
    private fun sp(ctx: Context, v: Float) = v * ctx.resources.displayMetrics.scaledDensity
}
