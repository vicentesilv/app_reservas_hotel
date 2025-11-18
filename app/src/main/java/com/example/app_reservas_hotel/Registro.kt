package com.example.app_reservas_hotel

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Registro : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        val TboxUsername = findViewById<EditText>(R.id.TboxUser)
        val TboxPassword = findViewById<EditText>(R.id.TboxUsuarioPassword)
        val TboxMail = findViewById<EditText>(R.id.Tboxmail)
        val TboxAge = findViewById<EditText>(R.id.Tboxage)
        val TboxPhone = findViewById<EditText>(R.id.Tboxphono)
        val BtonRegister = findViewById<Button>(R.id.BtnLogin)
        val BtonBackLogin = findViewById<Button>(R.id.btn_redirect_register)
        BtonRegister.setOnClickListener {
            val username=tranformTboxToString(TboxUsername)
            val password=tranformTboxToString(TboxPassword)
            val email=tranformTboxToString(TboxMail)
            val ageString=tranformTboxToString(TboxAge)
            val age=ageString.toInt()
            val phone=tranformTboxToString(TboxPhone)

            if(username.isNotEmpty() && password.isNotEmpty() && email.isNotEmpty() && ageString.isNotEmpty() && phone.isNotEmpty()) {
                attemptRegister(username, password, email, age, phone)
            }
            else{
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
        BtonBackLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun attemptRegister(username:String,password:String,mail:String,age:Int,phone:String){
        val dbHelper = DatabaseHelper(this)
        try {
            val success = dbHelper.registrarUsuario(mail,password,username,age,phone)
            if (success) {
                val intent = Intent(this, HotelesActivity::class.java)
                intent.putExtra("username", username)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Registro fallido", Toast.LENGTH_SHORT).show()
            }
        } finally {
            dbHelper.close()
        }
    }
}