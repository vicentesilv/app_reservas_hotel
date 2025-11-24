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
        // Mostrar sólo el número (la etiqueta "Disponible:" ya está en `numerodisponible`)
        tvNumber.text = roomModel.num.toString()
        tvType.text = roomModel.type
        // Usar recursos para formatear el precio (evita concatenación en setText)
        try {
            tvPrice.text = itemView.context.getString(R.string.price_format, roomModel.price)
        } catch (_: Exception) {
            // Fallback simple
            tvPrice.text = "${roomModel.price}"
        }
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

        // Click: abrir actividad de reserva pasando ids
        itemView.setOnClickListener {
            try {
                val ctx = itemView.context
                val intent = android.content.Intent(ctx, Class.forName("com.example.app_reservas_hotel.CrearReservaActivity") as Class<*>)
                intent.putExtra("ROOM_ID", roomModel.id)
                intent.putExtra("HOTEL_ID", roomModel.hotelId)
                intent.putExtra("ROOM_NUMBER", roomModel.num)
                // Nuevo: pasar tipo de habitación y nombre del hotel
                intent.putExtra("ROOM_TYPE", roomModel.type)
                intent.putExtra("HOTEL_NAME", roomModel.hotelName)
                ctx.startActivity(intent)
            } catch (e: ClassNotFoundException) {
                // Si la actividad no existe, no hacer nada (se guardará el error en logs si se desea)
            }
        }
    }
}