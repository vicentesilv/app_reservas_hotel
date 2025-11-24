package com.example.app_reservas_hotel.HotelRooms

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.content.edit
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_reservas_hotel.DatabaseHelper
import com.example.app_reservas_hotel.R
import com.google.android.material.navigation.NavigationView
import android.widget.ImageView
import com.example.app_reservas_hotel.utils.UiUtils

class HotelRoom : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var currentUsername: String? = null
    private lateinit var adapter: RoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habitaciones)

        // Asegurarse de que la base tenga datos reales (inserta datos de ejemplo si está vacía)
        val dbHelper = DatabaseHelper(this)
        dbHelper.insertSampleDataIfEmpty()

        // Configurar toolbar y botones (back + menú) usando utilidades
        UiUtils.setupToolbar(this, R.id.navbarRoom)

        val pair = UiUtils.initDrawer(this, R.id.drawer_layout_rooms, R.id.navigation_view_rooms)
        drawerLayout = pair.first ?: findViewById(R.id.drawer_layout_rooms)
        navigationView = pair.second ?: findViewById(R.id.navigation_view_rooms)

        // Inicializa header del drawer con el username (si viene)
        val header = navigationView.getHeaderView(0)
        val headerUsername = header?.findViewById<TextView>(R.id.headerUsername)
        val intentUsername = intent.getStringExtra("username")
        val storedUsername = UiUtils.getLoggedUsername(this)
        val username = if (!intentUsername.isNullOrEmpty()) intentUsername else storedUsername
        currentUsername = username
        headerUsername?.text = if (!username.isNullOrEmpty()) username else getString(R.string.header_default_user)

        UiUtils.bindBackButton(this, R.id.btnBackRooms)
        UiUtils.bindMenuButton(this, R.id.btnMenuRooms, drawerLayout)

        initRecyclerView()

        // conectar SearchView para búsqueda en tiempo real
        try {
            val searchView = findViewById<SearchView>(R.id.searchView)
            UiUtils.styleSearchView(searchView, this)

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    adapter.filter(query ?: "")
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    adapter.filter(newText ?: "")
                    return true
                }
            })
        } catch (_: Exception) {
            // Si no existe el SearchView, no hacer nada
        }

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

            // Obtener el nombre del hotel para mostrar en la reserva
            var hotelName: String? = null
            try {
                val hc = db.rawQuery("SELECT nombre FROM HOTELES WHERE id = ?", arrayOf(hotelId.toString()))
                hc.use { cursorH ->
                    if (cursorH.moveToFirst()) {
                        hotelName = try { cursorH.getString(0) } catch (_: Exception) { null }
                    }
                }
            } catch (_: Exception) {
                hotelName = null
            }

            val cursor = dbHelper.mostrarHabitacionesPorHotel(db, hotelId.toLong())
            cursor.use { c ->
                // Get column indices once, before the loop
                val idIndex = c.getColumnIndexOrThrow("id")
                val numIndex = c.getColumnIndexOrThrow("numero_habitacion")
                val typeIndex = c.getColumnIndexOrThrow("tipo")
                val priceIndex = c.getColumnIndexOrThrow("precio")
                val imageIndex = c.getColumnIndexOrThrow("foto")
                val capacityIndex = c.getColumnIndexOrThrow("capacidad")
                val descriptionIndex = c.getColumnIndexOrThrow("descripcion")

                if (c.moveToFirst()) {
                    do {
                        val idRoom = try { c.getLong(idIndex) } catch (_: Exception) { -1L }
                        val num = c.getInt(numIndex)
                        val type = c.getString(typeIndex)
                        val price = c.getDouble(priceIndex)

                        // Correctly handle nullable columns by checking for null before reading
                        val capacity = if (c.isNull(capacityIndex)) 0 else c.getInt(capacityIndex)
                        val image = if (c.isNull(imageIndex)) null else c.getString(imageIndex)
                        val description = if (c.isNull(descriptionIndex)) null else c.getString(descriptionIndex)

                        roomList.add(Room(idRoom, hotelId.toLong(), num, type, price, image, capacity, description, hotelName))
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
        adapter = RoomAdapter(roomList)
        recyclerView.adapter = adapter
    }

    override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_hoteles -> {
                drawerLayout.closeDrawers()
            }
            R.id.nav_reservas -> {
                try {
                    val cls = Class.forName("com.example.app_reservas_hotel.VerReservasActivity")
                    val intent = Intent(this, cls as Class<*>)
                    // pasar username (preferir currentUsername, si no usar SharedPreferences)
                    if (!currentUsername.isNullOrEmpty()) {
                        intent.putExtra("username", currentUsername)
                    } else {
                        val stored = UiUtils.getLoggedUsername(this)
                        stored?.let { intent.putExtra("username", it) }
                    }
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
                prefs.edit { remove("logged_username") }
                val intent = Intent(this, Class.forName("com.example.app_reservas_hotel.Login") as Class<*>)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        return true
    }
}