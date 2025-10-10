package com.example.dermahealth.helper

import android.content.Context
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.dermahealth.R
import com.example.dermahealth.adapter.RoutineAdapter

class SwipeToDeleteCallback(
    private val context: Context,
    private val adapter: RoutineAdapter,
    private val onDelete: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val cornerRadius = 36f  // more rounded
    private val bgInset = 24f       // space between card & background

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#F8BDBD") // slightly deeper pinkish red
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#B71C1C") // dark red for contrast
        textSize = 48f
        isAntiAlias = true
        typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false

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
        val height = itemView.bottom.toFloat() - itemView.top.toFloat()

        // calculate left/right draw limits with a bit of padding
        val left = itemView.left.toFloat() + if (dX > 0) bgInset else 0f
        val right = itemView.right.toFloat() - if (dX < 0) bgInset else 0f
        val top = itemView.top.toFloat() + 4f
        val bottom = itemView.bottom.toFloat() - 4f

        // define red rounded rect bounds based on swipe direction
        val background = if (dX > 0) {
            RectF(left, top, left + dX - bgInset, bottom)
        } else {
            RectF(right + dX + bgInset, top, right, bottom)
        }

        // draw rounded shape only on the visible part
        val path = Path().apply {
            addRoundRect(
                background,
                cornerRadius,
                cornerRadius,
                Path.Direction.CW
            )
        }
        c.save()
        c.clipPath(path)
        c.drawRect(background, backgroundPaint)
        c.restore()

        // Draw DELETE text
        val text = "Delete"
        val textWidth = textPaint.measureText(text)
        val textY = itemView.top + height / 2f + (textPaint.textSize / 3f)

        val textX = if (dX > 0)
            itemView.left + 64f
        else
            itemView.right - textWidth - 64f

        c.drawText(text, textX, textY, textPaint)

        // let RecyclerView handle item motion
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        onDelete(position)
    }
}
