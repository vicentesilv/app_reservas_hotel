package com.example.app_reservas_hotel.HotelRooms

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_reservas_hotel.DatabaseHelper
import com.example.app_reservas_hotel.R
import com.google.android.material.navigation.NavigationView

class HotelRoom : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var currentUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habitaciones)

        // Asegurarse de que la base tenga datos reales (inserta datos de ejemplo si está vacía)
        val dbHelper = DatabaseHelper(this)
        dbHelper.insertSampleDataIfEmpty()

        // Configurar toolbar y botones (back + menu)
        val toolbar = findViewById<Toolbar>(R.id.navbarRoom)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout_rooms)
        navigationView = findViewById<NavigationView>(R.id.navigation_view_rooms)
        navigationView.setNavigationItemSelectedListener(this)

        // Inicializar header del drawer con el username (si viene)
        val header = navigationView.getHeaderView(0)
        val headerUsername = header?.findViewById<TextView>(R.id.headerUsername)
        val intentUsername = intent.getStringExtra("username")
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val storedUsername = prefs.getString("logged_username", null)
        val username = if (!intentUsername.isNullOrEmpty()) intentUsername else storedUsername
        currentUsername = username
        headerUsername?.text = if (!username.isNullOrEmpty()) username else getString(R.string.header_default_user)

        val btnBack = findViewById<ImageButton>(R.id.btnBackRooms)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenuRooms)

        btnBack.setOnClickListener {
            // vuelve a la actividad anterior
            finish()
        }

        btnMenu.setOnClickListener {
            // abre el drawer navigation
            drawerLayout.openDrawer(GravityCompat.START)
        }

        initRecyclerView()

        // marcar un item por defecto (opcional)
        try {
            navigationView.setCheckedItem(R.id.nav_hoteles)
        } catch (_: Exception) {
        }
    }

    private fun initRecyclerView(){
        var hotelId = intent.getIntExtra("HOTEL_ID", -1)

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val roomList = mutableListOf<Room>()

        try {
            // Si no se pasó HOTEL_ID, intentar usar el primer hotel disponible
            if (hotelId == -1) {
                val hotelsCursor = dbHelper.mostrarHoteles(db)
                hotelsCursor.use { hc ->
                    if (hc.moveToFirst()) {
                        // columna id = 0 en la consulta mostrarHoteles
                        hotelId = try { hc.getInt(0) } catch (_: Exception) { -1 }
                        Log.d("HotelRoom", "No HOTEL_ID en intent; usando primer hotel con id=$hotelId")
                    } else {
                        Log.e("HotelRoom", "No hay hoteles en la base de datos.")
                    }
                }
            }

            if (hotelId == -1) {
                // No hay hotel válido para mostrar
                return
            }

            val cursor = dbHelper.mostrarHabitacionesPorHotel(db, hotelId.toLong())
            cursor.use { c ->
                // Get column indices once, before the loop
                val numIndex = c.getColumnIndexOrThrow("numero_habitacion")
                val typeIndex = c.getColumnIndexOrThrow("tipo")
                val priceIndex = c.getColumnIndexOrThrow("precio")
                val imageIndex = c.getColumnIndexOrThrow("foto")
                val capacityIndex = c.getColumnIndexOrThrow("capacidad")
                val descriptionIndex = c.getColumnIndexOrThrow("descripcion")

                if (c.moveToFirst()) {
                    do {
                        val num = c.getInt(numIndex)
                        val type = c.getString(typeIndex)
                        val price = c.getDouble(priceIndex)

                        // Correctly handle nullable columns by checking for null before reading
                        val capacity = if (c.isNull(capacityIndex)) 0 else c.getInt(capacityIndex)
                        val image = if (c.isNull(imageIndex)) null else c.getString(imageIndex)
                        val description = if (c.isNull(descriptionIndex)) null else c.getString(descriptionIndex)

                        roomList.add(Room(num, type, price, image, capacity, description))
                    } while (c.moveToNext())
                }
            }
        } catch (e: Exception) {
            // Log the specific error to understand what's failing
            Log.e("HotelRoom", "Error reading room data from database", e)
        } finally {
            db.close()
            dbHelper.close()
        }

        Log.d("HotelRoom", "Found ${roomList.size} rooms for hotel ID $hotelId. Populating adapter.")

        val recyclerView = findViewById<RecyclerView>(R.id.VistaHabitaciones)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = RoomAdapter(roomList)
    }

    override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
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
                    currentUsername?.let { intent.putExtra("username", it) }
                    startActivity(intent)
                } catch (_: ClassNotFoundException) {
                }
                drawerLayout.closeDrawers()
            }
            R.id.nav_logout -> {
                // limpiar sesión
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().remove("logged_username").apply()
                val intent = Intent(this, Class.forName("com.example.app_reservas_hotel.Login") as Class<*>)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        return true
    }
}