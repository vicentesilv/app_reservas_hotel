package com.example.app_reservas_hotel

import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.concurrent.thread

class VerReservasActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var rv: RecyclerView
    private lateinit var adapter: ReservasAdapter
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView
    private var currentUserId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_reservas)

        dbHelper = DatabaseHelper(this)

        val btnBack: ImageButton? = findViewById(R.id.btnBackVerReservas)
        btnBack?.setOnClickListener { finish() }

        rv = findViewById(R.id.rvReservas)
        adapter = ReservasAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        swipe = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshReservas)
        progress = findViewById(R.id.progressReservas)
        tvEmpty = findViewById(R.id.tvEmptyReservas)

        swipe.setOnRefreshListener(object : SwipeRefreshLayout.OnRefreshListener {
            override fun onRefresh() {
                cargarReservas()
            }
        })

        // Resolver user id: priorizar USER_ID pasado explícitamente
        var userId = intent.getLongExtra("USER_ID", -1L)

        // Si no llegó USER_ID, intentar con username pasado por Intent
        if (userId <= 0L) {
            val username = intent.getStringExtra("username")
            if (!username.isNullOrEmpty()) {
                try {
                    val cursor = dbHelper.obtenerUsuarioPorUsername(username)
                    cursor.use {
                        if (it.moveToFirst()) {
                            userId = try { it.getLong(0) } catch (_: Exception) { -1L }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }

        // Si aún no tenemos userId, intentar con username almacenado en SharedPreferences (sesión)
        if (userId <= 0L) {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val storedUsername = prefs.getString("logged_username", null)
            if (!storedUsername.isNullOrEmpty()) {
                try {
                    val cursor = dbHelper.obtenerUsuarioPorUsername(storedUsername)
                    cursor.use {
                        if (it.moveToFirst()) {
                            userId = try { it.getLong(0) } catch (_: Exception) { -1L }
                        }
                    }
                } catch (_: Exception) {
                }
            }

            // Añadir log que muestre valores usados para la resolución del usuario
            try {
                val intentUser = intent.getStringExtra("username")
                android.util.Log.d("VerReservasActivity", "resolveUser: intent.USER_ID=${intent.getLongExtra("USER_ID", -1L)}, intent.username=$intentUser, storedUsername=$storedUsername, resolvedUserId=$userId")
            } catch (_: Exception) {}
        }

        if (userId <= 0L) {
            // No se puede identificar al usuario: mostrar mensaje y salir del intento de carga
            progress.visibility = View.GONE
            swipe.isRefreshing = false
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Usuario no identificado"
            return
        }

        currentUserId = userId

        // Cargar reservas inicialmente
        cargarReservas()
    }

    private fun cargarReservas() {
        val userId = currentUserId
        if (userId <= 0L) {
            mostrarError("Usuario no identificado")
            swipe.setRefreshing(false)
            progress.visibility = View.GONE
            return
        }

        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        thread {
            val lista = mutableListOf<Reserva>()
            var db = dbHelper.readableDatabase
            var cursor: Cursor? = null
            try {
                cursor = dbHelper.mostrarReservasPorUsuario(db, userId)
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        do {
                            val id = c.getLong(0)
                            val idHotel = c.getLong(1)
                            val idHabitacion = c.getLong(2)
                            val nombre = c.getString(3)
                            val fechaEntrada = c.getString(4)
                            val fechaSalida = c.getString(5)
                            val numeroHabitacion = try { c.getInt(6) } catch (_: Exception) { 0 }

                            // Obtener nombre del hotel desde la tabla HOTELES
                            var hotelName = ""
                            try {
                                val hc = db.rawQuery("SELECT nombre FROM HOTELES WHERE id = ?", arrayOf(idHotel.toString()))
                                hc.use { hcursor ->
                                    if (hcursor.moveToFirst()) {
                                        hotelName = hcursor.getString(0) ?: ""
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("VerReservasActivity", "Error leyendo nombre de hotel", e)
                            }

                            lista.add(
                                Reserva(
                                    id = id,
                                    idHotel = idHotel,
                                    idHabitacion = idHabitacion,
                                    nombre = nombre ?: "",
                                    fechaEntrada = fechaEntrada ?: "",
                                    fechaSalida = fechaSalida ?: "",
                                    numeroHabitacion = numeroHabitacion,
                                    hotelName = hotelName
                                )
                            )
                        } while (c.moveToNext())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { cursor?.close() } catch (_: Exception) {}
                try { db.close() } catch (_: Exception) {}
            }

            runOnUiThread {
                progress.visibility = View.GONE
                swipe.setRefreshing(false)
                if (lista.isEmpty()) {
                    adapter.submitList(emptyList())
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "No hay reservas"
                } else {
                    tvEmpty.visibility = View.GONE
                    adapter.submitList(lista)
                }
            }
        }
    }

    private fun mostrarError(msg: String) {
        tvEmpty.text = msg
        tvEmpty.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
