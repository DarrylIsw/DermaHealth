package com.example.dermahealth.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.dermahealth.R

class CarouselAdapter(private val images: List<Int>) :
    RecyclerView.Adapter<CarouselAdapter.CarouselVH>() {

    // ViewHolder class: holds the views for each carousel item
    inner class CarouselVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iv: ImageView = itemView.findViewById(R.id.iv_carousel_item) // ImageView in item layout
    }

    // Called when RecyclerView needs a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselVH {
        // Inflate the item layout (XML) for each carousel item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel, parent, false) // safe way to inflate layout
        return CarouselVH(view)
    }

    // Bind data to the ViewHolder
    override fun onBindViewHolder(holder: CarouselVH, position: Int) {
        // Calculate index to loop images infinitely
        val idx = position % images.size
        holder.iv.setImageResource(images[idx]) // Set image resource for ImageView
    }

    // Return a very large number to create infinite scrolling effect
    override fun getItemCount(): Int = Int.MAX_VALUE
}
