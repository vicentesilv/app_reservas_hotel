package com.example.app_reservas_hotel.HotelRooms

data class Room(
    val id: Long,
    val hotelId: Long,
    val num: Int,
    val type: String,
    val price: Double,
    val image: String?,
    val capacity: Int,
    val description: String?,
    val hotelName: String?
)
