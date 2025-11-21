package com.example.app_reservas_hotel.HotelRooms

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_reservas_hotel.R

class HotelRoom : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RoomProvider.Rooms
        setContentView(R.layout.activity_habitaciones)
        initRecyclerView()
    }
    private fun initRecyclerView(){
        val recyclerView = findViewById<RecyclerView>(R.id.VistaHabitaciones)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = RoomAdapter(RoomProvider.Rooms)
    }
}