package com.example.app_reservas_hotel

// Modelo simple para representar un hotel
// imageResId: recurso drawable opcional
// imagePath: ruta/archivo (por ejemplo en assets) opcional
data class Hotel(
    val id: Int,
    val name: String,
    val address: String,
    val phone: String,
    val imageResId: Int? = null,
    val imagePath: String? = null
)
