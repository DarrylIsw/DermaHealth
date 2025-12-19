package com.example.dermahealth.adapter

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.dermahealth.R
import com.example.dermahealth.data.ScanImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HistoryImagesAdapter(
    private val images: List<ScanImage>
) : RecyclerView.Adapter<HistoryImagesAdapter.ImgVH>() {

    inner class ImgVH(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val tvScore: TextView = view.findViewById(R.id.tvScore)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImgVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_image, parent, false)
        return ImgVH(view)
    }

    override fun getItemCount() = images.size

    override fun onBindViewHolder(holder: ImgVH, position: Int) {
        val item = images[position]

        // Load image with Coil (thumbnail)
        holder.ivImage.load(File(item.path)) {
            crossfade(true)
            placeholder(R.drawable.bg_image_placeholder)
            error(R.drawable.bg_image_placeholder)
            transformations(RoundedCornersTransformation(12f))
        }

        // Show score as percentage
        val scorePct = ((item.score ?: 0f) * 100).toInt()
        holder.tvScore.text = "$scorePct%"

        // Show formatted date/time from timestamp
        holder.tvDate.text = formatTimestamp(item.timestamp)

        // Zoom on click
        holder.ivImage.setOnClickListener {
            showZoomDialog(holder.itemView.context, item.path)
        }
    }

    private fun formatTimestamp(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Unknown"

        return try {
            // Parse incoming format: 2025-12-19T21:10:00Z
            val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            // Display format
            val outFmt = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())

            val date = inFmt.parse(isoString) ?: return "Unknown"
            outFmt.format(date)
        } catch (_: Exception) {
            // fallback: show raw string if parsing fails
            isoString
        }
    }

    // ----------------- ZOOM DIALOG -----------------

    private fun showZoomDialog(context: Context, path: String) {
        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

        val iv = ImageView(context).apply {
            setBackgroundColor(Color.BLACK)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER

            // Load full image tanpa rounded corners
            load(File(path)) {
                crossfade(true)
                placeholder(R.drawable.bg_image_placeholder)
                error(R.drawable.bg_image_placeholder)
            }

            // Tap sekali untuk close
            setOnClickListener { dialog.dismiss() }
        }

        dialog.setContentView(iv)

        // Fullscreen
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.black)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }

        dialog.show()
    }
}
