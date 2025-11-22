package com.example.app_reservas_hotel.HotelRooms

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_reservas_hotel.DatabaseHelper
import com.example.app_reservas_hotel.R

class HotelRoom : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habitaciones)
        initRecyclerView()
    }

    private fun initRecyclerView(){
        val hotelId = intent.getIntExtra("HOTEL_ID", -1)
        val currentUsername=intent.getStringExtra("username")
        if (hotelId == -1) {
            Log.e("HotelRoom", "Error: No HOTEL_ID was provided in the intent.")
            return
        }

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val roomList = mutableListOf<Room>()

        try {
            val cursor = dbHelper.mostrarHabitacionesPorHotel(db, hotelId.toLong())
            cursor.use { c ->
                // Get column indices once, before the loop
                val numIndex = c.getColumnIndexOrThrow("numero_habitacion")
                val typeIndex = c.getColumnIndexOrThrow("tipo")
                val priceIndex = c.getColumnIndexOrThrow("precio")
                val imageIndex = c.getColumnIndexOrThrow("foto")
                val capacityIndex = c.getColumnIndexOrThrow("capacidad")
                val descriptionIndex = c.getColumnIndexOrThrow("descripcion")

                if (c.moveToFirst()) {
                    do {
                        val num = c.getInt(numIndex)
                        val type = c.getString(typeIndex)
                        val price = c.getDouble(priceIndex)
                        
                        // Correctly handle nullable columns by checking for null before reading
                        val capacity = if (c.isNull(capacityIndex)) 0 else c.getInt(capacityIndex)
                        val image = if (c.isNull(imageIndex)) null else c.getString(imageIndex)
                        val description = if (c.isNull(descriptionIndex)) null else c.getString(descriptionIndex)

                        roomList.add(Room(num, type, price, image, capacity, description))
                    } while (c.moveToNext())
                }
            }
        } catch (e: Exception) {
            // Log the specific error to understand what's failing
            Log.e("HotelRoom", "Error reading room data from database", e)
        } finally {
            db.close()
            dbHelper.close()
        }
        
        Log.d("HotelRoom", "Found ${roomList.size} rooms for hotel ID $hotelId. Populating adapter.")

        val recyclerView = findViewById<RecyclerView>(R.id.VistaHabitaciones)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = RoomAdapter(roomList)
    }
}