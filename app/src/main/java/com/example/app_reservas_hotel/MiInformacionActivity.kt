package com.example.app_reservas_hotel

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.text.InputType
import android.view.ViewGroup
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AlertDialog

class MiInformacionActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mi_informacion)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Obtener username de SharedPreferences o Intent
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val storedUsername = prefs.getString("logged_username", null)
        val intentUsername = intent.getStringExtra("username")
        val username = if (!intentUsername.isNullOrEmpty()) intentUsername else storedUsername

        // Si no hay sesión activa redirigir al Login
        if (username.isNullOrEmpty()) {
            val toLogin = Intent(this, Login::class.java)
            toLogin.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(toLogin)
            finish()
            return
        }

        // Configurar header
        val header: View? = navigationView.getHeaderView(0)
        val headerUsername = header?.findViewById<TextView>(R.id.headerUsername)
        headerUsername?.text = username

        // Drawer toggle
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        try {
            toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.black)
        } catch (_: Exception) {}

        // Referencias UI
        val imgAvatar = findViewById<ImageView>(R.id.imgAvatar)
        val txtUser = findViewById<TextView>(R.id.txtUsername)
        val txtSubtitle = findViewById<TextView>(R.id.txtSubtitle)
        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val txtAge = findViewById<TextView>(R.id.txtAge)
        val txtNumber = findViewById<TextView>(R.id.txtNumber)
        val btnEdit = findViewById<Button>(R.id.btnEditProfile)

        // Ajustes visuales
        txtUser.text = username
        txtSubtitle.text = getString(R.string.account_label)
        btnEdit.text = getString(R.string.editar_info)
        imgAvatar.setImageResource(R.drawable.ic_user)

        // Cargar datos desde la base de datos
        val dbHelper = DatabaseHelper(this)
        val cursor = try {
            dbHelper.obtenerUsuarioPorUsername(username)
        } catch (_: Exception) {
            null
        }

        if (cursor != null) {
            cursor.use {
                if (it.moveToFirst()) {
                    val name = try { it.getString(2) } catch (_: Exception) { null }
                    val age = try { it.getInt(3) } catch (_: Exception) { -1 }
                    val number = try { it.getString(4) } catch (_: Exception) { null }

                    txtNombre.text = if (!name.isNullOrEmpty()) name else getString(R.string.dash)
                    txtAge.text = if (age >= 0) age.toString() else getString(R.string.dash)
                    txtNumber.text = if (!number.isNullOrEmpty()) number else getString(R.string.dash)
                } else {
                    txtNombre.text = getString(R.string.dash)
                    txtAge.text = getString(R.string.dash)
                    txtNumber.text = getString(R.string.dash)
                }
            }
        } else {
            txtNombre.text = getString(R.string.dash)
            txtAge.text = getString(R.string.dash)
            txtNumber.text = getString(R.string.dash)
        }
        try { dbHelper.close() } catch (_: Exception) {}

        // Editar perfil: mostrar diálogo con campos nombre, edad, teléfono
        btnEdit.setOnClickListener {
            // Editar solo: contraseña y teléfono. No prellenamos la contraseña por seguridad.
            val numberPrefill = if (txtNumber.text.toString() == getString(R.string.dash)) "" else txtNumber.text.toString()

            val passwordInput = EditText(this).apply {
                hint = "Contraseña"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            val numberInput = EditText(this).apply {
                hint = getString(R.string.label_telefono)
                setText(numberPrefill)
                inputType = InputType.TYPE_CLASS_PHONE
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 8, 16, 8)
                addView(passwordInput)
                addView(numberInput)
            }

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.editar_info))
                .setView(container)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    val newPass = passwordInput.text.toString().trim().ifEmpty { null }
                    val newNumber = numberInput.text.toString().trim().ifEmpty { null }

                    if (newPass == null && newNumber == null) {
                        // nada que actualizar
                        android.widget.Toast.makeText(this, "No hay cambios", android.widget.Toast.LENGTH_SHORT).show()
                        // mantener el diálogo abierto brevemente no es posible aquí; simplemente cerrar
                        dialog.dismiss()
                        return@setPositiveButton
                    }

                    val dbUpdate = DatabaseHelper(this)
                    val success = try {
                        dbUpdate.actualizarCredencialesPorUsername(username, newPass, newNumber)
                    } catch (ex: Exception) {
                        false
                    }
                    try { dbUpdate.close() } catch (_: Exception) {}

                    if (success) {
                        // solo actualizar UI del número; no mostramos contraseña
                        txtNumber.text = newNumber ?: getString(R.string.dash)
                        android.widget.Toast.makeText(this, "Información actualizada", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(this, "No se pudo actualizar la información", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

         // marcar menú
         navigationView.setCheckedItem(R.id.nav_mi_info)
     }

     override fun onOptionsItemSelected(item: MenuItem): Boolean {
         return super.onOptionsItemSelected(item)
     }

     override fun onNavigationItemSelected(item: MenuItem): Boolean {
         when (item.itemId) {
             R.id.nav_hoteles -> {
                 try {
                     val cls = Class.forName("com.example.app_reservas_hotel.HotelesActivity")
                     val intent = Intent(this, cls as Class<*>)
                     startActivity(intent)
                 } catch (_: ClassNotFoundException) {}
                 drawerLayout.closeDrawers()
             }
             R.id.nav_reservas -> {
                 try {
                     val cls = Class.forName("com.example.app_reservas_hotel.VerReservasActivity")
                     val intent = Intent(this, cls as Class<*>)
                    // pasar username desde SharedPreferences si existe
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    val storedUsername = prefs.getString("logged_username", null)
                    storedUsername?.let { intent.putExtra("username", it) }
                     startActivity(intent)
                 } catch (_: ClassNotFoundException) {}
                 drawerLayout.closeDrawers()
             }
             R.id.nav_mi_info -> {
                 drawerLayout.closeDrawers()
             }
             R.id.nav_logout -> {
                 // limpiar sesión
                 val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                 prefs.edit().remove("logged_username").apply()
                 val intent = Intent(this, Login::class.java)
                 intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                 startActivity(intent)
                 finish()
             }
         }
         return true
     }
 }
