package com.example.app_reservas_hotel.HotelRooms

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app_reservas_hotel.R

class RoomViewHolder(view: View): RecyclerView.ViewHolder(view) {
    private var ivRoom = view.findViewById<ImageView>(R.id.ivRoom)
    private var tvNumber= view.findViewById<TextView>(R.id.tvNumber)
    private var tvType= view.findViewById<TextView>(R.id.tvType)
    private var tvPrice= view.findViewById<TextView>(R.id.tvPrice)
    private var tvDescription= view.findViewById<TextView>(R.id.tvDescription)
    private var tvCapacity= view.findViewById<TextView>(R.id.tvCapacity)

    fun render(roomModel: Room){
        tvNumber.text = "#${roomModel.num}"
        tvType.text = roomModel.type
        tvPrice.text= "$${roomModel.price} USD"
        tvDescription.text = roomModel.description ?: "Descripción no disponible."
        tvCapacity.text = if (roomModel.capacity > 0) "Capacidad: ${roomModel.capacity}" else ""

        // Helper to normalize path and build final asset URI
        fun buildAssetPath(path: String?): String? {
            if (path.isNullOrEmpty()) return null
            // Normalize path by removing duplicates
            var normalizedPath = path.replace("\\", "/").trim()
            while (normalizedPath.contains("images/images/")) {
                normalizedPath = normalizedPath.replace("images/images/", "images/")
            }
            return "file:///android_asset/$normalizedPath"
        }

        val finalAssetPath = buildAssetPath(roomModel.image)

        Glide.with(ivRoom.context)
            .load(finalAssetPath)
            .placeholder(R.drawable.ic_launcher_background) // Image shown while loading
            .error(R.drawable.ic_launcher_background)     // Image shown on error (or if path is null)
            .centerCrop()
            .into(ivRoom)
    }
}