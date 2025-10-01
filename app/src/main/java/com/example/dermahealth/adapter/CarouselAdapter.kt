package com.example.dermahealth.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.dermahealth.R

class CarouselAdapter(private val images: List<Int>) :
    RecyclerView.Adapter<CarouselAdapter.CarouselVH>() {

    inner class CarouselVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iv: ImageView = itemView.findViewById(R.id.iv_carousel_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel, parent, false) // ⬅️ uses XML, safe
        return CarouselVH(view)
    }

    override fun onBindViewHolder(holder: CarouselVH, position: Int) {
        val idx = position % images.size
        holder.iv.setImageResource(images[idx])
    }

    override fun getItemCount(): Int = Int.MAX_VALUE // infinite scroll effect
}
