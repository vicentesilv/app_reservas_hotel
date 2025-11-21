package com.example.app_reservas_hotel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HotelAdapter(
    private val originalItems: List<Hotel>,
    private val onItemClick: (Hotel) -> Unit // Callback para el clic
) : RecyclerView.Adapter<HotelAdapter.VH>() {

    // lista mutable que representa los elementos visibles (filtrados)
    private val items: MutableList<Hotel> = originalItems.toMutableList()

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.hotelImage)
        val name: TextView = itemView.findViewById(R.id.hotelName)
        val addr: TextView = itemView.findViewById(R.id.hotelAddr)
        val phone: TextView = itemView.findViewById(R.id.hotelPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.hotel_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val h = items[position]
        holder.name.text = h.name
        holder.addr.text = h.address
        holder.phone.text = h.phone

        // Asignar el listener al item entero
        holder.itemView.setOnClickListener {
            onItemClick(h)
        }

        val ctx = holder.itemView.context
        val placeholder = R.drawable.ic_launcher_foreground

        // Si existe imagePath, intentar con Glide (URL o asset)
        if (!h.imagePath.isNullOrEmpty()) {
            try {
                val path = h.imagePath
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    Glide.with(ctx).load(path).centerCrop().placeholder(placeholder).into(holder.img)
                } else {
                    // Intentar como asset
                    val assetUri = "file:///android_asset/$path"
                    Glide.with(ctx).load(assetUri).centerCrop().placeholder(placeholder).into(holder.img)
                }
                return
            } catch (_: Exception) {
                // si falla, seguiremos con los fallbacks
            }
        }

        // Si existe imageResId, usarlo
        if (h.imageResId != null) {
            try {
                Glide.with(ctx).load(h.imageResId).centerCrop().placeholder(placeholder).into(holder.img)
                return
            } catch (_: Exception) {
            }
        }

        // Último recurso: placeholder
        try {
            Glide.with(ctx).load(placeholder).centerCrop().into(holder.img)
        } catch (_: Exception) {
            // nada más que hacer
        }
    }

    override fun getItemCount(): Int = items.size

    // Filtrar por nombre y dirección (case-insensitive)
    fun filter(query: String) {
        val q = query.trim().lowercase()
        items.clear()
        if (q.isEmpty()) {
            items.addAll(originalItems)
        } else {
            for (h in originalItems) {
                val name = h.name?.lowercase() ?: ""
                val addr = h.address?.lowercase() ?: ""
                if (name.contains(q) || addr.contains(q)) {
                    items.add(h)
                }
            }
        }
        notifyDataSetChanged()
    }
}
