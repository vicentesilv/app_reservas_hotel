package com.example.app_reservas_hotel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.EditText

fun tranformTboxToString(Object: EditText): String{
    val text = Object.text.toString().trim()
    return text
}
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = findViewById<LinearLayout>(R.id.container)

        fun normalize(path: String?): String? {
            if (path.isNullOrEmpty()) return null
            var p = path.replaceFirst("^images/images/".toRegex(), "images/")
            if (!p.startsWith("images/")) p = "images/" + p
            return p
        }

        fun assetExists(path: String): Boolean {
            return try {
                assets.open(path).close()
                true
            } catch (e: Exception) {
                Log.w(TAG, "Asset no encontrado: $path", e)
                false
            }
        }

        // Leer la BD en background y poblar la UI
        Thread {
            try {
                val dbHelper = DatabaseHelper(this@MainActivity)
                val db = dbHelper.readableDatabase

                // --- Mostrar usuarios desde la tabla `usuarios` ---
                try {
                    val usersCursor = db.rawQuery("SELECT id, username, name, age, number FROM usuarios", null)
                    usersCursor.use { uc ->
                        if (uc.moveToFirst()) {
                            runOnUiThread {
                                val header = TextView(this@MainActivity)
                                header.text = "Usuarios"
                                header.textSize = 18f
                                container.addView(header)
                            }

                            val idIdx = uc.getColumnIndex("id")
                            val usernameIdx = uc.getColumnIndex("username")
                            val nameIdx = uc.getColumnIndex("name")
                            val ageIdx = uc.getColumnIndex("age")
                            val numberIdx = uc.getColumnIndex("number")

                            do {
                                val uid = if (idIdx >= 0) try { uc.getLong(idIdx) } catch (_: Exception) { -1L } else -1L
                                val uname = if (usernameIdx >= 0) try { uc.getString(usernameIdx) ?: "" } catch (_: Exception) { "" } else ""
                                val realName = if (nameIdx >= 0) try { uc.getString(nameIdx) ?: "" } catch (_: Exception) { "" } else ""
                                val age = if (ageIdx >= 0) try { uc.getInt(ageIdx) } catch (_: Exception) { -1 } else -1
                                val number = if (numberIdx >= 0) try { uc.getString(numberIdx) ?: "" } catch (_: Exception) { "" } else ""

                                // Contar reservas del usuario (no muestra datos sensibles)
                                var reservasCount = 0
                                try {
                                    val rcCursor = db.rawQuery("SELECT COUNT(*) FROM reservas WHERE id_usuario = ?", arrayOf(uid.toString()))
                                    rcCursor.use { rcc ->
                                        if (rcc.moveToFirst()) reservasCount = rcc.getInt(0)
                                    }
                                } catch (_: Exception) {
                                    // ignora si la tabla reservas no existe o falla la consulta
                                }

                                runOnUiThread {
                                    // Mostrar en dos líneas: primera línea identificación, segunda con datos adicionales
                                    val tv = TextView(this@MainActivity)
                                    val line1 = StringBuilder()
                                    if (uid >= 0) line1.append("#$uid ")
                                    if (uname.isNotEmpty()) line1.append(uname)

                                    val line2 = StringBuilder()
                                    if (realName.isNotEmpty()) line2.append("Nombre: " + realName)
                                    if (age >= 0) {
                                        if (line2.isNotEmpty()) line2.append(" | ")
                                        line2.append("Edad: " + age)
                                    }
                                    if (number.isNotEmpty()) {
                                        if (line2.isNotEmpty()) line2.append(" | ")
                                        line2.append("Tel: " + number)
                                    }
                                    // Añadir número de reservas si hay al menos una
                                    if (reservasCount > 0) {
                                        if (line2.isNotEmpty()) line2.append(" | ")
                                        line2.append("Reservas: " + reservasCount)
                                    }

                                    tv.text = if (line2.isNotEmpty()) line1.toString() + "\n" + line2.toString() else line1.toString()
                                    container.addView(tv)
                                }
                            } while (uc.moveToNext())
                        } else {
                            runOnUiThread {
                                val tv = TextView(this@MainActivity)
                                tv.text = "No hay usuarios en la base de datos."
                                container.addView(tv)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error al leer usuarios", e)
                }

                // --- Mostrar hoteles desde la tabla `hoteles` ---
                val cursor = dbHelper.mostrarHoteles(db)

                cursor.use { c ->
                    if (c.moveToFirst()) {
                        do {
                            val hid = c.getLong(0)
                            val hname = c.getString(1)
                            val haddr = c.getString(2)
                            val hphone = c.getString(3)
                            val rawHfoto = c.getString(4)
                            val hfoto = normalize(rawHfoto)

                            // Inflar layout del hotel
                            val view = LayoutInflater.from(this@MainActivity).inflate(R.layout.hotel_item, container, false)
                            val img = view.findViewById<ImageView>(R.id.hotelImage)
                            val nameTv = view.findViewById<TextView>(R.id.hotelName)
                            val addrTv = view.findViewById<TextView>(R.id.hotelAddr)
                            val phoneTv = view.findViewById<TextView>(R.id.hotelPhone)
                            val roomsContainer = view.findViewById<LinearLayout>(R.id.roomsContainer)

                            nameTv.text = hname
                            addrTv.text = "Dirección: $haddr"
                            phoneTv.text = "Tel: $hphone"

                            // Seleccionar imagen: hotel o primera habitación disponible
                            var selectedPath: String? = null
                            if (!hfoto.isNullOrEmpty() && assetExists(hfoto)) selectedPath = hfoto

                            val roomsCursor = dbHelper.mostrarHabitacionesPorHotel(db, hid)
                            roomsCursor.use { rc ->
                                if (rc.moveToFirst()) {
                                    do {
                                        val num = rc.getInt(1)
                                        val tipo = rc.getString(2)
                                        val precio = rc.getDouble(3)
                                        val rfoto = normalize(rc.getString(4))

                                        // Si no hay foto seleccionada, usar la primera habitacion que tenga imagen
                                        if (selectedPath == null && !rfoto.isNullOrEmpty() && assetExists(rfoto)) {
                                            selectedPath = rfoto
                                        }

                                        // Añadir fila de habitación (texto simple)
                                        val roomTv = TextView(this@MainActivity)
                                        roomTv.text = "    #$num - $tipo - \$${"%.2f".format(precio)}"
                                        roomTv.textSize = 14f
                                        roomsContainer.addView(roomTv)
                                    } while (rc.moveToNext())
                                } else {
                                    val emptyTv = TextView(this@MainActivity)
                                    emptyTv.text = "    (Sin habitaciones)"
                                    roomsContainer.addView(emptyTv)
                                }
                            }

                            // Mostrar la imagen en el hilo UI leyendo desde assets (evita dependencia externa)
                            runOnUiThread {
                                if (selectedPath != null) {
                                    try {
                                        assets.open(selectedPath).use { ins ->
                                            val bmp = BitmapFactory.decodeStream(ins)
                                            if (bmp != null) img.setImageBitmap(bmp) else img.setImageResource(android.R.drawable.ic_menu_report_image)
                                        }
                                    } catch (e: Exception) {
                                        img.setImageResource(android.R.drawable.ic_menu_report_image)
                                    }
                                } else {
                                    img.setImageResource(android.R.drawable.ic_menu_report_image)
                                }

                                container.addView(view)
                            }

                        } while (c.moveToNext())
                    } else {
                        runOnUiThread {
                            val tv = TextView(this@MainActivity)
                            tv.text = "No hay hoteles en la base de datos."
                            container.addView(tv)
                        }
                    }
                }

                db.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error al leer la base de datos", e)
                runOnUiThread {
                    val tv = TextView(this@MainActivity)
                    val msg = e.message ?: "Desconocido"
                    tv.text = "Error al acceder a la base de datos: $msg"
                    container.addView(tv)
                }
            }
        }.start()

        // Mantener la redirección al login tras 5s
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@MainActivity, Registro::class.java))
            finish()
        }, 5000L)
    }
}