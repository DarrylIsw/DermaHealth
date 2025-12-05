package com.example.dermahealth.adapter

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.dermahealth.R
import com.example.dermahealth.data.ScanImage
import java.io.File

class HistoryImagesAdapter(
    private val images: List<ScanImage>
) : RecyclerView.Adapter<HistoryImagesAdapter.ImgVH>() {

    inner class ImgVH(val img: ImageView) : RecyclerView.ViewHolder(img)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImgVH {
        val context = parent.context

        val iv = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(180, 180)  // Horizontal thumbnails
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(8, 8, 8, 8)
            clipToOutline = true
        }
        return ImgVH(iv)
    }

    override fun getItemCount() = images.size

    override fun onBindViewHolder(holder: ImgVH, position: Int) {
        val item = images[position]

        holder.img.load(File(item.path)) {
            crossfade(true)
            placeholder(R.drawable.bg_image_placeholder)
            error(R.drawable.bg_image_placeholder)
            transformations(RoundedCornersTransformation(12f))
        }
    }
}
