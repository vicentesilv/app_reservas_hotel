// File: `app/src/main/java/com/example/app_reservas_hotel/Login.kt`
package com.example.app_reservas_hotel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Login : AppCompatActivity() {
    private val TAG = "login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val BtnLogin = findViewById<Button>(R.id.BtnLogin)
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
    }

    private fun attemptLogin(username: String, password: String) {
        val dbHelper = DatabaseHelper(this)
        try {
            val success = dbHelper.iniciarSesion(username, password)
            if (success) {
                val intent = Intent(this, HotelesActivity::class.java)
                intent.putExtra("username", username)
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