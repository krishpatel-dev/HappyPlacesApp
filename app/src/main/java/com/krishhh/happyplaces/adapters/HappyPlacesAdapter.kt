package com.krishhh.happyplaces.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.krishhh.happyplaces.activities.AddHappyPlaceActivity
import com.krishhh.happyplaces.activities.MainActivity
import com.krishhh.happyplaces.databinding.ItemHappyPlaceBinding
import com.krishhh.happyplaces.models.HappyPlaceModel

// Adapter class to bind the list of HappyPlaceModel to the RecyclerView
class HappyPlacesAdapter(
    private val context: Context,
    private var list: ArrayList<HappyPlaceModel>
) : RecyclerView.Adapter<HappyPlacesAdapter.MyViewHolder>() {

    private var onClickListener: OnClickListener? = null

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

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    // Called by RecyclerView to display data at the specified position
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = list[position] // Get the current item from the list

        if(holder is MyViewHolder) {
            holder.binding.ivPlaceImage.setImageURI(Uri.parse(model.image)) // Load image from URI
            holder.binding.tvTitle.text = model.title                        // Set title
            holder.binding.tvDescription.text = model.description            // Set description

            holder.itemView.setOnClickListener{
                if(onClickListener != null){
                    onClickListener!!.onClick(position, model)
                }
            }
        }
    }

    // A function to edit the added happy place detail and pass the existing details through intent.
    fun notifyEditItem(activity: Activity, position: Int, requestCode: Int) {
        val intent = Intent(context, AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, list[position])
        activity.startActivityForResult(intent, requestCode)

        notifyItemChanged(position) // Notify any registered observers that the item at position has changed.
    }


    override fun getItemCount(): Int {
        return list.size
    }

    interface OnClickListener {
        fun onClick(position: Int, model: HappyPlaceModel)
    }

    // Custom ViewHolder class that holds the binding object for the layout
    class MyViewHolder(val binding: ItemHappyPlaceBinding) : RecyclerView.ViewHolder(binding.root)
}
