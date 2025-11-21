package com.example.app_reservas_hotel.HotelRooms

data class Room(val num: Int,
                val type: String,
                val price: Int,
                val image: String,
                val capacity: Int,
                val description: String)