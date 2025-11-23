package com.example.app_reservas_hotel.HotelRooms

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.app_reservas_hotel.R

class RoomAdapter(private val originalRooms: List<Room>): RecyclerView.Adapter<RoomViewHolder>() {
    // lista mutable que se muestra actualmente
    private val rooms: MutableList<Room> = originalRooms.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val layoutInflater= LayoutInflater.from(parent.context)
        return RoomViewHolder(layoutInflater.inflate(R.layout.item_room,parent,false))
    }

    override fun onBindViewHolder(holder: RoomViewHolder,position: Int) {
        val item = rooms[position]
        holder.render(item)
    }

    override fun getItemCount(): Int {
        return rooms.size
    }

    // Filtra por número, tipo o descripción (insensible a mayúsculas)
    fun filter(query: String) {
        val q = query.trim().lowercase()
        rooms.clear()
        if (q.isEmpty()) {
            rooms.addAll(originalRooms)
        } else {
            for (r in originalRooms) {
                // buscar en número (convertido a texto), tipo y descripción
                val numStr = "#${r.num}".lowercase()
                val type = r.type.lowercase()
                val desc = (r.description ?: "").lowercase()
                if (numStr.contains(q) || type.contains(q) || desc.contains(q)) {
                    rooms.add(r)
                }
            }
        }
        notifyDataSetChanged()
    }
}