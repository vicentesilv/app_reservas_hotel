// File: `app/src/main/java/com/example/app_reservas_hotel/HotelesActivity.kt`
package com.example.app_reservas_hotel

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_reservas_hotel.HotelRooms.HotelRoom
import com.google.android.material.navigation.NavigationView
import kotlin.jvm.java

class HotelesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var adapter: HotelAdapter
    private var currentUsername: String? = null

    // Handler y runnable para ocultar el mensaje de bienvenida y permitir su cancelación
    private val mainHandler = Handler(Looper.getMainLooper())
    private var welcomeRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hoteles)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Inicializar header del drawer con el username (si viene)
        val header: View? = navigationView.getHeaderView(0)
        val headerUsername = header?.findViewById<TextView>(R.id.headerUsername)
        // Preferir username pasado por Intent, si no existe usar SharedPreferences (sesión)
        val intentUsername = intent.getStringExtra("username")
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val storedUsername = prefs.getString("logged_username", null)
        val username = if (!intentUsername.isNullOrEmpty()) intentUsername else storedUsername
        currentUsername = username
        headerUsername?.text = if (!username.isNullOrEmpty()) username else getString(R.string.header_default_user)

        // ActionBarDrawerToggle usando strings para accesibilidad
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Asegurar que el "hamburger" sea negro para ser visible sobre fondo blanco
        try {
            toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.black)
        } catch (_: Exception) {
            // si falla por alguna razón, no detener la app
        }

        val txtWelcome = findViewById<TextView>(R.id.txtWelcome)
        txtWelcome?.text = if (!username.isNullOrEmpty()) getString(R.string.welcome_user, username) else getString(R.string.welcome)

        // Ocultar el texto de bienvenida 2 segundos después de iniciar sesión
        txtWelcome?.let { tv ->
            welcomeRunnable = Runnable {
                // usar GONE para que no ocupe espacio
                tv.visibility = View.GONE
            }
            mainHandler.postDelayed(welcomeRunnable!!, 2000)
        }

        // RecyclerView
        val recycler = findViewById<RecyclerView>(R.id.recyclerHotels)
        recycler.layoutManager = LinearLayoutManager(this)
        // Divider
        val divider = androidx.recyclerview.widget.DividerItemDecoration(this, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL)
        recycler.addItemDecoration(divider)

        // Cargar hoteles desde la base de datos
        val dbHelper = DatabaseHelper(this)

        // Normalizar rutas ya guardadas (corrige 'images/images/...' u otros prefijos erróneos)
        try {
            dbHelper.normalizeFotoPathsInDb()
        } catch (_: Exception) {
        }

        val db = dbHelper.readableDatabase
        val hotelsList = mutableListOf<Hotel>()
        try {
            val cursor = dbHelper.mostrarHoteles(db)
            cursor.use {
                if (it.moveToFirst()) {
                    do {
                        // columnas: id, nombre, direccion, telefono, foto
                        val nombre = it.getString(1) ?: ""
                        val direccion = it.getString(2) ?: ""
                        val telefono = it.getString(3) ?: ""
                        val fotoRaw = try { it.getString(4) } catch (_: Exception) { null }

                        // Normalizar ruta: eliminar duplicados "images/images/", eliminar prefijos inesperados
                        fun normalizeAssetPath(p: String?): String? {
                            if (p.isNullOrEmpty()) return null
                            var s = p.replace("\\", "/").trim()
                            // eliminar prefijo file:///android_asset/ si existe
                            s = s.removePrefix("file:///android_asset/")
                            // eliminar prefijo / si existe
                            while (s.startsWith("/")) s = s.removePrefix("/")
                            // colapsar repeticiones de "images/"
                            while (s.contains("images/images/")) s = s.replace("images/images/", "images/")
                            // si quedó un prefijo doble como "images/./" eliminar
                            s = s.replace("./", "")
                            return s
                        }

                        val foto = normalizeAssetPath(fotoRaw)

                        val hotel = Hotel(
                            name = nombre,
                            address = direccion,
                            phone = telefono,
                            imageResId = null,
                            imagePath = if (!foto.isNullOrEmpty()) foto else null
                        )
                        hotelsList.add(hotel)
                    } while (it.moveToNext())
                }
            }
        } catch (_: Exception) {
            // en caso de error dejamos la lista vacía
        } finally {
            try { db.close() } catch (_: Exception) {}
            try { dbHelper.close() } catch (_: Exception) {}
        }

        // Inicializar adapter con la lista completa
        adapter = HotelAdapter(hotelsList){hotel->
            val intent = Intent(this, HotelRoom::class.java)
            intent.putExtra("hotel", hotel.name)
            startActivity(intent)
            finish()
        }
        recycler.adapter = adapter

        // conectar SearchView para búsqueda en tiempo real
        try {
            val searchView = findViewById<SearchView>(R.id.searchView)

            // Ajustes visuales: icono de lupa y colores de texto/hint para fondo oscuro
            try {
                // cambiar icono de lupa
                val magIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
                magIcon?.setImageResource(R.drawable.ic_search)
                magIcon?.setColorFilter(ContextCompat.getColor(this, R.color.black))

                // cambiar color del texto y hint a negro
                val searchEditText = searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
                searchEditText?.setTextColor(ContextCompat.getColor(this, R.color.black))
                searchEditText?.setHintTextColor(ContextCompat.getColor(this, R.color.black))

                // ajustar el fondo del campo interno (quita el fondo por defecto para que el nuestro sea el visible)
                val plate = searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)
                plate?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                // ajustar padding para que el texto no quede pegado
                searchView.setPadding(8, 0, 8, 0)
            } catch (_: Exception) {
            }

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
            // si no existe el SearchView, no hacer nada
        }

        // ejemplo: click en item (por ahora sólo un placeholder)
        // podrías exponer un callback desde el adaptador para abrir detalle
        recycler.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {

        })

        // marcar Hoteles como seleccionado
        navigationView.setCheckedItem(R.id.nav_hoteles)
    }

    override fun onDestroy() {
        // Cancelar callback pendiente para evitar fugas
        welcomeRunnable?.let { mainHandler.removeCallbacks(it) }
        super.onDestroy()
    }

    // Eliminado onCreateOptionsMenu para quitar el menú de la derecha (overflow)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_hoteles -> {
                // ya estamos aquí
                return true
            }
            R.id.nav_reservas -> {
                try {
                    val cls = Class.forName("com.example.app_reservas_hotel.MisReservasActivity")
                    val intent = Intent(this, cls as Class<*>)
                    startActivity(intent)
                } catch (_: ClassNotFoundException) {
                }
                return true
            }
            R.id.nav_mi_info -> {
                try {
                    val cls = Class.forName("com.example.app_reservas_hotel.MiInformacionActivity")
                    val intent = Intent(this, cls as Class<*>)
                    // pasar username actual si existe
                    currentUsername?.let { intent.putExtra("username", it) }
                    startActivity(intent)
                } catch (_: ClassNotFoundException) {
                }
                return true
            }
            R.id.nav_logout -> {
                // limpiar sesión
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().remove("logged_username").apply()
                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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
                    // actividad no existente: no hacer nada
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
                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        return true
    }
}