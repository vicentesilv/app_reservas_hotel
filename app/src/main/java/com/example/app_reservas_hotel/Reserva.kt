package com.example.app_reservas_hotel

data class Reserva(
    val id: Long,
    val idHotel: Long,
    val idHabitacion: Long,
    val nombre: String,
    val fechaEntrada: String,
    val fechaSalida: String,
    val numeroHabitacion: Int,
    val hotelName: String = ""
)
