package com.example.app_reservas_hotel.HotelRooms

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class RoomAdapter(private val rooms: List<Room>): RecyclerView.Adapter<RoomViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(
        holder: RoomViewHolder,
        position: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        return rooms.size
    }
}