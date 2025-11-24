package com.example.app_reservas_hotel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.CalendarView
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.*
import com.example.app_reservas_hotel.utils.UiUtils

class CrearReservaActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var roomId: Long = -1L
    private var hotelId: Long = -1L
    private var roomNumber: Int = -1

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    // fechas en formato yyyy-MM-dd
    private var fechaEntrada: String = ""
    private var fechaSalida: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_reserva)

        // Extras
        roomId = intent.getLongExtra("ROOM_ID", -1L)
        hotelId = intent.getLongExtra("HOTEL_ID", -1L)
        roomNumber = intent.getIntExtra("ROOM_NUMBER", -1)
        val roomType = intent.getStringExtra("ROOM_TYPE") ?: "-"
        val hotelName = intent.getStringExtra("HOTEL_NAME") ?: "-"

        // Configurar toolbar (usando UiUtils)
        UiUtils.setupToolbar(this, R.id.toolbar)

        // Drawer and navigation view (UiUtils se encarga de setNavigationItemSelectedListener si corresponde)
        val pair = UiUtils.initDrawer(this, R.id.drawer_layout, R.id.navigation_view_reserva)
        drawerLayout = pair.first ?: findViewById(R.id.drawer_layout)
        navigationView = pair.second ?: findViewById(R.id.navigation_view_reserva)

        // Botones del toolbar: usar utilidades para evitar duplicar lógica
        UiUtils.bindBackButton(this, R.id.btnBackReserva)
        UiUtils.bindMenuButton(this, R.id.btnMenuReserva, drawerLayout)

        // Poner título en el toolbar similar a activity_hoteles (mostrar en TextView también)
        val tvRoomHeader = findViewById<TextView>(R.id.tvRoomHeader)
        tvRoomHeader.text = getString(R.string.room_header_format, hotelName, roomType)

        // Resto del inicializador (fechas, botones, etc.) — ahora usando CalendarView inline
        val tvFechaEntrada = findViewById<TextView>(R.id.tvFechaEntrada)
        val tvFechaSalida = findViewById<TextView>(R.id.tvFechaSalida)
        val etName = findViewById<EditText>(R.id.etName)
        val btnReservar = findViewById<Button>(R.id.btnReservar)
        val tvSelectionHelp = findViewById<TextView>(R.id.tvSelectionHelp)
        val calendarView = findViewById<CalendarView>(R.id.calendarViewReserva)

        // Inicializar fechas: hoy y mañana
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayMillis = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val tomorrowMillis = cal.timeInMillis

        // Usar utilitario para manejar selector de rango y actualizaciones UI
        UiUtils.attachCalendarRangeSelector(
            this,
            calendarView,
            tvFechaEntrada,
            tvFechaSalida,
            tvSelectionHelp,
            todayMillis,
            tomorrowMillis
        ) { entradaStr, salidaStr ->
            fechaEntrada = entradaStr
            fechaSalida = salidaStr
        }

        // Reservar
        btnReservar.setOnClickListener {
            val nombre = etName.text.toString().trim()
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Introduce tu nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (roomId == -1L || hotelId == -1L) {
                Toast.makeText(this, "Datos de habitación inválidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fechaEntrada.isEmpty() || fechaSalida.isEmpty()) {
                Toast.makeText(this, "Selecciona fechas de entrada y salida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fechaEntrada > fechaSalida) {
                Toast.makeText(this, "La fecha de entrada no puede ser posterior a la de salida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Resolver usuario actual desde SharedPreferences (utilitario)
            val storedUsername = UiUtils.getLoggedUsername(this)
            if (storedUsername.isNullOrEmpty()) {
                Toast.makeText(this, "Debes iniciar sesión para crear una reserva", Toast.LENGTH_SHORT).show()
                // redirigir al login
                try {
                    val cls = Class.forName("com.example.app_reservas_hotel.Login")
                    val intent = Intent(this, cls as Class<*>)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } catch (_: Exception) {}
                return@setOnClickListener
            }

            val dbHelper = DatabaseHelper(this)
            var userIdForReservation: Long = -1L
            try {
                val cursor = dbHelper.obtenerUsuarioPorUsername(storedUsername)
                cursor.use {
                    if (it.moveToFirst()) {
                        userIdForReservation = try { it.getLong(0) } catch (_: Exception) { -1L }
                    }
                }
            } catch (_: Exception) {
            }

            if (userIdForReservation <= 0L) {
                Toast.makeText(this, "Usuario no encontrado en la base de datos", Toast.LENGTH_SHORT).show()
                try { dbHelper.close() } catch (_: Exception) {}
                return@setOnClickListener
            }

            val success = dbHelper.crearReserva(hotelId, roomId, userIdForReservation, nombre, fechaEntrada, fechaSalida, roomNumber)
            try { dbHelper.close() } catch (_: Exception) {}

            if (success) {
                Toast.makeText(this, "Reserva creada", Toast.LENGTH_SHORT).show()
                // Opcionalmente abrir la pantalla de mis reservas pasando el username
                try {
                    val cls = Class.forName("com.example.app_reservas_hotel.VerReservasActivity")
                    val intent = Intent(this, cls as Class<*>)
                    intent.putExtra("username", storedUsername)
                    startActivity(intent)
                } catch (_: ClassNotFoundException) {
                    // si no existe, solo cerrar
                }
                finish()
            } else {
                Toast.makeText(this, "Error creando reserva", Toast.LENGTH_SHORT).show()
                Log.e("CrearReservaActivity", "crearReserva returned false")
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val storedUsername = UiUtils.getLoggedUsername(this)
        return UiUtils.handleNavigationSelection(this, item.itemId, drawerLayout, storedUsername)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
