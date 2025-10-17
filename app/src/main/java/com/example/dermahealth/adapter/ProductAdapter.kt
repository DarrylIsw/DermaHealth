package com.example.dermahealth.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dermahealth.R
import com.example.dermahealth.databinding.ItemProductBinding
import com.example.dermahealth.data.Product

class ProductAdapter(private val products: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // ViewHolder holds references to item views using ViewBinding
    inner class ProductViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Called when RecyclerView needs a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false // Do not attach to parent immediately
        )
        return ProductViewHolder(binding)
    }

    // Bind data to each item
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        // Set product name and brand with safe fallbacks
        holder.binding.tvProductName.text = product.name ?: "Unknown"
        holder.binding.tvProductBrand.text = product.brand ?: ""

        // Load product image with Glide, using a placeholder while loading
        Glide.with(holder.binding.ivProductImage.context)
            .load(product.imageUrl)                     // Load URL or resource
            .placeholder(R.drawable.ic_placeholder)     // Placeholder image
            .into(holder.binding.ivProductImage)        // Target ImageView
    }

    // Return total number of products
    override fun getItemCount() = products.size
}
