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

        // Configurar toolbar (usando el mismo id que en activity_hoteles)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Drawer and navigation view
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view_reserva)
        try {
            navigationView.setNavigationItemSelectedListener(this)
        } catch (_: Exception) {
        }

        // Botones del toolbar
        val btnBack = findViewById<ImageButton?>(R.id.btnBackReserva)
        val btnMenu = findViewById<ImageButton?>(R.id.btnMenuReserva)

        btnBack?.setOnClickListener {
            finish()
        }

        btnMenu?.setOnClickListener {
            // abrir drawer
            try {
                drawerLayout.openDrawer(GravityCompat.START)
                return@setOnClickListener
            } catch (_: Exception) {
            }
            Toast.makeText(this, "Abrir menú", Toast.LENGTH_SHORT).show()
        }

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

        fechaEntrada = sdf.format(Date(todayMillis))
        fechaSalida = sdf.format(Date(tomorrowMillis))

        tvFechaEntrada.text = getString(R.string.fecha_with_value, getString(R.string.btn_fecha_entrada), fechaEntrada)
        tvFechaSalida.text = getString(R.string.fecha_with_value, getString(R.string.btn_fecha_salida), fechaSalida)

        // Aplicar estilo inicial: Entrada seleccionada por defecto
        tvFechaEntrada.setTextColor(ContextCompat.getColor(this, R.color.primary))
        tvFechaEntrada.setTypeface(null, Typeface.BOLD)
        tvFechaSalida.setTextColor(ContextCompat.getColor(this, R.color.on_surface))
        tvFechaSalida.setTypeface(null, Typeface.NORMAL)
        tvSelectionHelp.setTextColor(ContextCompat.getColor(this, R.color.secondary_text))

        // Configurar CalendarView
        calendarView.minDate = todayMillis
        calendarView.date = todayMillis // mostrar hoy seleccionado por defecto

        // Estado: true = elegir Entrada, false = elegir Salida
        var selectingEntrada = true

        // Clicks en TextViews para alternar target de selección
        tvFechaEntrada.setOnClickListener {
            selectingEntrada = true
            // destacar visiblemente usando color y estilo
            tvFechaEntrada.setTextColor(ContextCompat.getColor(this, R.color.primary))
            tvFechaEntrada.setTypeface(null, Typeface.BOLD)
            tvFechaSalida.setTextColor(ContextCompat.getColor(this, R.color.on_surface))
            tvFechaSalida.setTypeface(null, Typeface.NORMAL)
            tvSelectionHelp.setTextColor(ContextCompat.getColor(this, R.color.secondary_text))
            tvSelectionHelp.text = getString(R.string.select_dates)
        }

        tvFechaSalida.setOnClickListener {
            selectingEntrada = false
            tvFechaSalida.setTextColor(ContextCompat.getColor(this, R.color.primary))
            tvFechaSalida.setTypeface(null, Typeface.BOLD)
            tvFechaEntrada.setTextColor(ContextCompat.getColor(this, R.color.on_surface))
            tvFechaEntrada.setTypeface(null, Typeface.NORMAL)
            tvSelectionHelp.setTextColor(ContextCompat.getColor(this, R.color.secondary_text))
            tvSelectionHelp.text = getString(R.string.select_dates)
        }

        // Listener del calendario: al seleccionar una fecha, actualizar la fecha correspondiente
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selCal = Calendar.getInstance()
            selCal.set(Calendar.YEAR, year)
            selCal.set(Calendar.MONTH, month)
            selCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selCal.set(Calendar.HOUR_OF_DAY, 0)
            selCal.set(Calendar.MINUTE, 0)
            selCal.set(Calendar.SECOND, 0)
            selCal.set(Calendar.MILLISECOND, 0)
            val selMillis = selCal.timeInMillis
            val selDateStr = sdf.format(Date(selMillis))

            if (selectingEntrada) {
                fechaEntrada = selDateStr
                // Si la entrada seleccionada es después de la salida actual, ajustar salida a entrada+1 día
                val entDate = selCal.timeInMillis
                val outCal = Calendar.getInstance()
                try {
                    // parse fechaSalida existente
                    outCal.time = sdf.parse(fechaSalida) ?: Date(entDate + 24*60*60*1000)
                } catch (_: Exception) {
                    outCal.timeInMillis = entDate + 24*60*60*1000
                }
                if (entDate >= outCal.timeInMillis) {
                    outCal.timeInMillis = entDate + 24*60*60*1000
                    fechaSalida = sdf.format(Date(outCal.timeInMillis))
                    tvFechaSalida.text = getString(R.string.fecha_with_value, getString(R.string.btn_fecha_salida), fechaSalida)
                }
                tvFechaEntrada.text = getString(R.string.fecha_with_value, getString(R.string.btn_fecha_entrada), fechaEntrada)
            } else {
                fechaSalida = selDateStr
                // Si la salida es anterior o igual a la entrada, ajustar entrada a salida-1 día
                val outDate = selCal.timeInMillis
                val inCal = Calendar.getInstance()
                try {
                    inCal.time = sdf.parse(fechaEntrada) ?: Date(outDate - 24*60*60*1000)
                } catch (_: Exception) {
                    inCal.timeInMillis = outDate - 24*60*60*1000
                }
                if (outDate <= inCal.timeInMillis) {
                    inCal.timeInMillis = outDate - 24*60*60*1000
                    fechaEntrada = sdf.format(Date(inCal.timeInMillis))
                    tvFechaEntrada.text = getString(R.string.fecha_with_value, getString(R.string.btn_fecha_entrada), fechaEntrada)
                }
                tvFechaSalida.text = getString(R.string.fecha_with_value, getString(R.string.btn_fecha_salida), fechaSalida)
            }

            // Mostrar resumen
            tvSelectionHelp.text = getString(R.string.rango_seleccionado, fechaEntrada, fechaSalida)
            tvSelectionHelp.setTextColor(ContextCompat.getColor(this, R.color.highlight))
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

            val dbHelper = DatabaseHelper(this)
            val success = dbHelper.crearReserva(hotelId, roomId, -1L, nombre, fechaEntrada, fechaSalida, roomNumber)
            if (success) {
                Toast.makeText(this, "Reserva creada", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error creando reserva", Toast.LENGTH_SHORT).show()
                Log.e("CrearReservaActivity", "crearReserva returned false")
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_hoteles -> {
                drawerLayout.closeDrawers()
            }
            R.id.nav_reservas -> {
                try {
                    val cls = Class.forName("com.example.app_reservas_hotel.MisReservasActivity")
                    val intent = Intent(this, cls as Class<*>)
                    startActivity(intent)
                } catch (_: ClassNotFoundException) {
                }
                drawerLayout.closeDrawers()
            }
            R.id.nav_mi_info -> {
                try {
                    val cls = Class.forName("com.example.app_reservas_hotel.MiInformacionActivity")
                    val intent = Intent(this, cls as Class<*>)
                    startActivity(intent)
                } catch (_: ClassNotFoundException) {
                }
                drawerLayout.closeDrawers()
            }
            R.id.nav_logout -> {
                // limpiar sesión
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit { remove("logged_username") }
                try {
                    val cls = Class.forName("com.example.app_reservas_hotel.Login")
                    val intent = Intent(this, cls as Class<*>)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } catch (_: Exception) {
                }
            }
        }
        return true
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
