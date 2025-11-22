package com.example.app_reservas_hotel.HotelRooms

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.app_reservas_hotel.R

class RoomAdapter(private val rooms: List<Room>): RecyclerView.Adapter<RoomViewHolder>() {
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
}