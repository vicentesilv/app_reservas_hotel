// File: `app/src/main/java/com/example/app_reservas_hotel/Login.kt`
package com.example.app_reservas_hotel

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app_reservas_hotel.HotelRooms.HotelRoom

class Login : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val BtnLogin = findViewById<Button>(R.id.BtnLogin)
        val BtnRegistro = findViewById<Button>(R.id.btn_redirect_register)
        val TboxUser = findViewById<EditText>(R.id.TboxUser)
        val TboxPassword = findViewById<EditText>(R.id.TboxUsuarioPassword)

        BtnLogin.setOnClickListener {
            val username = TboxUser.text.toString().trim()
            val password = TboxPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                attemptLogin(username, password)
            } else {
                Toast.makeText(this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
            }
        }
        BtnRegistro.setOnClickListener {
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
        }
    }

    private fun attemptLogin(username: String, password: String) {
        val dbHelper = DatabaseHelper(this)
        try {
            val success = dbHelper.iniciarSesion(username, password)
            if (success) {
                // guardar sesión para que otras Activities puedan leer el usuario
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().putString("logged_username", username).apply()
                val intent = Intent(this, HotelRoom::class.java)
                //intent.putExtra("username", username)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        } finally {
            dbHelper.close()
        }
    }
}