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

class HistoryImagesAdapter(
    private val images: List<ScanImage>
) : RecyclerView.Adapter<HistoryImagesAdapter.ImgVH>() {

    inner class ImgVH(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val tvScore: TextView = view.findViewById(R.id.tvScore)
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

        // 🔍 Zoom on click
        holder.ivImage.setOnClickListener {
            showZoomDialog(holder.itemView.context, item.path)
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
