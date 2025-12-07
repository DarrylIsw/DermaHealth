package com.example.dermahealth.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        // Load image with Coil
        holder.ivImage.load(File(item.path)) {
            crossfade(true)
            placeholder(R.drawable.bg_image_placeholder)
            error(R.drawable.bg_image_placeholder)
            transformations(RoundedCornersTransformation(12f))
        }

        // Show score as percentage
        val scorePct = ((item.score ?: 0f) * 100).toInt()
        holder.tvScore.text = "$scorePct%"
    }

}
