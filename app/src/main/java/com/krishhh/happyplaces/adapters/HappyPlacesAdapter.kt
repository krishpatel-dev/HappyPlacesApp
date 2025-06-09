package com.krishhh.happyplaces.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.krishhh.happyplaces.databinding.ItemHappyPlaceBinding
import com.krishhh.happyplaces.models.HappyPlaceModel

// Adapter class to bind the list of HappyPlaceModel to the RecyclerView
class HappyPlacesAdapter(
    private val context: Context,
    private var list: ArrayList<HappyPlaceModel>
) : RecyclerView.Adapter<HappyPlacesAdapter.MyViewHolder>() {

    // Called when RecyclerView needs a new ViewHolder of the given type to represent an item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            ItemHappyPlaceBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    // Called by RecyclerView to display data at the specified position
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = list[position] // Get the current item from the list

        // Bind the model data to the views using ViewBinding
        holder.binding.ivPlaceImage.setImageURI(Uri.parse(model.image)) // Load image from URI
        holder.binding.tvTitle.text = model.title                        // Set title
        holder.binding.tvDescription.text = model.description            // Set description
    }

    override fun getItemCount(): Int {
        return list.size
    }

    // Custom ViewHolder class that holds the binding object for the layout
    class MyViewHolder(val binding: ItemHappyPlaceBinding) : RecyclerView.ViewHolder(binding.root)
}
