package com.example.app_reservas_hotel

import android.app.AlertDialog
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import com.example.app_reservas_hotel.utils.UiUtils

class VerReservasActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var rv: RecyclerView
    private lateinit var adapter: ReservasAdapter
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView
    private var currentUserId: Long = -1L

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_reservas)

        dbHelper = DatabaseHelper(this)

        UiUtils.bindBackButton(this, R.id.btnBackVerReservas)

        rv = findViewById(R.id.rvReservas)
        adapter = ReservasAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        progress = findViewById(R.id.progressReservas)
        tvEmpty = findViewById(R.id.tvEmptyReservas)

        // Conectar callbacks para editar y eliminar
        adapter.onEdit = { reserva ->
            showEditDialog(reserva)
        }
        adapter.onDelete = { reserva ->
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_reserva_title))
                .setMessage(getString(R.string.delete_reserva_message))
                .setPositiveButton(android.R.string.yes) { dialog, _ ->
                    dialog.dismiss()
                    progress.visibility = View.VISIBLE
                    thread {
                        val ok = try {
                            dbHelper.cancelarReservaConRestauracion(reserva.id)
                        } catch (e: Exception) {
                            Log.e("VerReservasActivity", "Error al eliminar reserva", e)
                            false
                        }
                        runOnUiThread {
                            progress.visibility = View.GONE
                            if (ok) {
                                Toast.makeText(this, getString(R.string.reserva_eliminada), Toast.LENGTH_SHORT).show()
                                cargarReservas()
                            } else {
                                Toast.makeText(this, getString(R.string.reserva_eliminada_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton(android.R.string.no, null)
                .show()
        }

        // Resolver user id y cargar
        resolveUserAndLoad()
    }

    private fun resolveUserAndLoad() {
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
                } catch (_: Exception) {}
            }
        }

        // Si aún no tenemos userId, intentar con username almacenado en SharedPreferences (sesión)
        if (userId <= 0L) {
            val storedUsername = UiUtils.getLoggedUsername(this)
            if (!storedUsername.isNullOrEmpty()) {
                try {
                    val cursor = dbHelper.obtenerUsuarioPorUsername(storedUsername)
                    cursor.use {
                        if (it.moveToFirst()) {
                            userId = try { it.getLong(0) } catch (_: Exception) { -1L }
                        }
                    }
                } catch (_: Exception) {}
            }
            try {
                val intentUser = intent.getStringExtra("username")
                Log.d("VerReservasActivity", "resolveUser: intent.USER_ID=${intent.getLongExtra("USER_ID", -1L)}, intent.username=$intentUser, storedUsername=$storedUsername, resolvedUserId=$userId")
            } catch (_: Exception) {}
        }

        if (userId <= 0L) {
            progress.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = getString(R.string.usuario_no_identificado)
            return
        }

        currentUserId = userId
        cargarReservas()
    }

    private fun showEditDialog(reserva: Reserva) {
        try {
            val inflater = LayoutInflater.from(this)
            val view = inflater.inflate(R.layout.dialog_edit_reserva, null)

            // Nuevos IDs del layout con un único CalendarView
            val tvFechaEntrada = view.findViewById<TextView>(R.id.tvFechaEntrada)
            val tvFechaSalida = view.findViewById<TextView>(R.id.tvFechaSalida)
            val tvSelectionHelp = view.findViewById<TextView>(R.id.tvSelectionHelp)
            val calendarView = view.findViewById<CalendarView>(R.id.calendarViewReserva)
            val btnUpdate = view.findViewById<Button>(R.id.btnUpdateDates)

            // Parsear fechas actuales
            var entradaMillis = try { dateFormat.parse(reserva.fechaEntrada)?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() }
            var salidaMillis = try { dateFormat.parse(reserva.fechaSalida)?.time ?: (entradaMillis + 24*60*60*1000) } catch (_: Exception) { entradaMillis + 24*60*60*1000 }

            // Usar utilitario para manejar interacciones del calendario y TextViews
            UiUtils.attachCalendarRangeSelector(
                this,
                calendarView,
                tvFechaEntrada,
                tvFechaSalida,
                tvSelectionHelp,
                entradaMillis,
                salidaMillis
            ) { newEntrada, newSalida ->
                entradaMillis = try { dateFormat.parse(newEntrada)?.time ?: entradaMillis } catch (_: Exception) { entradaMillis }
                salidaMillis = try { dateFormat.parse(newSalida)?.time ?: salidaMillis } catch (_: Exception) { salidaMillis }
            }

            val dialog = AlertDialog.Builder(this)
                .setView(view)
                .create()

            btnUpdate.setOnClickListener {
                // Validar rango
                if (entradaMillis > salidaMillis) {
                    Toast.makeText(this, getString(R.string.fecha_entrada_posterior_error), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val nuevaEntrada = dateFormat.format(Date(entradaMillis))
                val nuevaSalida = dateFormat.format(Date(salidaMillis))

                progress.visibility = View.VISIBLE
                thread {
                    val ok = try {
                        dbHelper.actualizarFechasReserva(reserva.id, nuevaEntrada, nuevaSalida)
                    } catch (e: Exception) {
                        Log.e("VerReservasActivity", "Error actualizando fechas", e)
                        false
                    }

                    runOnUiThread {
                        progress.visibility = View.GONE
                        if (ok) {
                            Toast.makeText(this, getString(R.string.fechas_actualizadas), Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            cargarReservas()
                        } else {
                            Toast.makeText(this, getString(R.string.fechas_actualizadas_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e("VerReservasActivity", "showEditDialog error", e)
        }
    }

    private fun cargarReservas() {
        val userId = currentUserId
        if (userId <= 0L) {
            mostrarError(R.string.usuario_no_identificado)
            progress.visibility = View.GONE
            return
        }

        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        thread {
            val lista = mutableListOf<Reserva>()
            val db = dbHelper.readableDatabase
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
                if (lista.isEmpty()) {
                    adapter.submitList(emptyList())
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = getString(R.string.no_reservas)
                } else {
                    tvEmpty.visibility = View.GONE
                    adapter.submitList(lista)
                }
            }
        }
    }

    private fun mostrarError(msgResId: Int) {
        tvEmpty.text = getString(msgResId)
        tvEmpty.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
