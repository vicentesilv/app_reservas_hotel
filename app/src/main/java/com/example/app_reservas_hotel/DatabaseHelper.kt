package com.example.app_reservas_hotel

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, dataBaseName, null, databaseVersion) {
    companion object {
        private const val dataBaseName = "hotel_reservas.db"
        private const val databaseVersion = 2
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            """
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    name TEXT,
                    age INTEGER,
                    number TEXT
                );
                """.trimIndent()
        )
        db?.execSQL(
            """
                CREATE TABLE IF NOT EXISTS HOTELES (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    direccion TEXT NOT NULL,
                    telefono TEXT NOT NULL,
                    foto TEXT
                );
                """.trimIndent()
        )
        db?.execSQL(
            """
                CREATE TABLE IF NOT EXISTS habitaciones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_hotel INTEGER NOT NULL,
                    numero_habitacion INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    precio REAL NOT NULL,
                    foto TEXT
                );
                """.trimIndent()
        )
        db?.execSQL(
            """
                CREATE TABLE IF NOT EXISTS reservas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_hotel INTEGER NOT NULL,
                    id_habitacion INTEGER NOT NULL,
                    id_usuario INTEGER NOT NULL,
                    nombre TEXT NOT NULL,
                    fecha_entrada TEXT NOT NULL,
                    fecha_salida TEXT NOT NULL,
                    numero_habitacion INTEGER NOT NULL
                );
                """.trimIndent()
        )

        db?.let {
            insertFromAssets(it)
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS reservas")
        db?.execSQL("DROP TABLE IF EXISTS habitaciones")
        db?.execSQL("DROP TABLE IF EXISTS HOTELES")
        db?.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    /**
     * Intenta leer `assets/data.json` y poblar la base.
     */
    private fun insertFromAssets(db: SQLiteDatabase): Boolean {
        val jsonString: String = try {
            context.assets.open("data.json").bufferedReader().use { it.readText() }
        } catch (_: IOException) {
            return false
        }

        // Helper para normalizar rutas de assets/imagenes
        fun normalizeAssetPath(p: String?): String? {
            if (p.isNullOrEmpty()) return null
            var s = p.replace("\\", "/").trim()
            s = s.removePrefix("file:///android_asset/")
            while (s.startsWith("/")) s = s.removePrefix("/")
            while (s.contains("images/images/")) s = s.replace("images/images/", "images/")
            s = s.replace("./", "")
            return s
        }

        try {
            val root = JSONObject(jsonString)
            val hotelesArray = root.optJSONArray("hoteles") ?: JSONArray()
            val usuariosArray = root.optJSONArray("usuarios") ?: JSONArray()

            var insertedAny = false
            db.beginTransaction()
            try {
                // Insertar hoteles y habitaciones
                for (i in 0 until hotelesArray.length()) {
                    val hObj = hotelesArray.optJSONObject(i) ?: continue
                    val nombre = hObj.optString("nombre", "Hotel desconocido")
                    val direccion = hObj.optString("direccion", "Dirección desconocida")
                    val telefono = hObj.optString("telefono", "000-000-000")
                    val fotoHotelRaw = hObj.optString("foto", "")
                    val fotoHotel = normalizeAssetPath(fotoHotelRaw)

                    val hv = ContentValues().apply {
                        put("nombre", nombre)
                        put("direccion", direccion)
                        put("telefono", telefono)
                        if (!fotoHotel.isNullOrEmpty()) {
                            put("foto", fotoHotel)
                        }
                    }

                    val hotelId = db.insert("HOTELES", null, hv)
                    if (hotelId == -1L) continue
                    insertedAny = true

                    val habitacionesArray = hObj.optJSONArray("habitaciones") ?: JSONArray()
                    for (j in 0 until habitacionesArray.length()) {
                        val rObj = habitacionesArray.optJSONObject(j) ?: continue
                        val numero = rObj.optInt("numero_habitacion", j + 1)
                        val tipo = rObj.optString("tipo", "Estándar")
                        val precio = rObj.optDouble("precio", 50.0)
                        val fotoHabitacionRaw = rObj.optString("foto", "")
                        val fotoHabitacion = normalizeAssetPath(fotoHabitacionRaw)

                        val rv = ContentValues().apply {
                            put("id_hotel", hotelId)
                            put("numero_habitacion", numero)
                            put("tipo", tipo)
                            put("precio", precio)
                            if (!fotoHabitacion.isNullOrEmpty()) {
                                put("foto", fotoHabitacion)
                            }
                        }
                        db.insert("habitaciones", null, rv)
                    }
                }

                // Insertar usuarios
                for (i in 0 until usuariosArray.length()) {
                    val uObj = usuariosArray.optJSONObject(i) ?: continue
                    val username = uObj.optString("username", "")
                    if (username.isEmpty()) continue
                    val password = uObj.optString("password", "")
                    val name = uObj.optString("name", "")
                    val age = if (uObj.has("age")) uObj.optInt("age", -1) else -1
                    val number = uObj.optString("number", "")
                    val uv = ContentValues().apply {
                        put("username", username)
                        put("password", password)
                        if (!name.isNullOrEmpty()) put("name", name)
                        if (age >= 0) put("age", age)
                        if (!number.isNullOrEmpty()) put("number", number)
                    }
                    db.insertWithOnConflict("usuarios", null, uv, SQLiteDatabase.CONFLICT_IGNORE)
                    insertedAny = true
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            return insertedAny
        } catch (_: JSONException) {
            return false
        }
    }

    private fun hasData(db: SQLiteDatabase, table: String): Boolean {
        val cursor: Cursor = db.rawQuery("SELECT COUNT(*) FROM $table", null)
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) > 0 else false
        }
    }

    fun mostrarDatosPrueba(db: SQLiteDatabase): String {
        val sb = StringBuilder()
        val usuariosCursor = db.rawQuery("SELECT id, username, name, age, number FROM usuarios", null)
        usuariosCursor.use {
            if (it.moveToFirst()) {
                sb.append("Usuarios registrados:\n")
                do {
                    val uid = it.getLong(0)
                    val username = it.getString(1)
                    val name = it.getString(2)
                    val age = try { it.getInt(3) } catch (_: Exception) { -1 }
                    val number = it.getString(4)
                    val parts = mutableListOf<String>()
                    if (!name.isNullOrEmpty()) parts.add("Nombre: $name")
                    if (age >= 0) parts.add("Edad: $age")
                    if (!number.isNullOrEmpty()) parts.add("Tel: $number")
                    sb.append(" - #$uid $username")
                    if (parts.isNotEmpty()) sb.append(" (" + parts.joinToString(" | ") + ")")
                    sb.append("\n")
                } while (it.moveToNext())
            } else {
                sb.append("No hay usuarios registrados.\n")
            }
        }

        val hotelsCursor = db.rawQuery("SELECT id, nombre, direccion, telefono, foto FROM HOTELES", null)
        hotelsCursor.use { hc ->
            if (hc.moveToFirst()) {
                do {
                    val hid = hc.getLong(0)
                    val hname = hc.getString(1)
                    val haddr = hc.getString(2)
                    val hphone = hc.getString(3)
                    val hfoto = hc.getString(4)
                    sb.append("Hotel: $hname\n")
                    sb.append("  Dirección: $haddr\n")
                    sb.append("  Teléfono: $hphone\n")
                    sb.append("  Foto: ${hfoto ?: "Sin foto"}\n")

                    val roomsCursor = db.rawQuery(
                        "SELECT numero_habitacion, tipo, precio, foto FROM habitaciones WHERE id_hotel = ?",
                        arrayOf(hid.toString())
                    )
                    roomsCursor.use { rc ->
                        if (rc.moveToFirst()) {
                            sb.append("  Habitaciones:\n")
                            do {
                                val num = rc.getInt(0)
                                val tipo = rc.getString(1)
                                val precio = rc.getDouble(2)
                                val rfoto = rc.getString(3)
                                sb.append("    #$num - $tipo - \$${"%.2f".format(precio)} - Foto: ${rfoto ?: "Sin foto"}\n")
                            } while (rc.moveToNext())
                        } else {
                            sb.append("  (Sin habitaciones)\n")
                        }
                    }
                    sb.append("\n")
                } while (hc.moveToNext())
            } else {
                sb.append("No hay hoteles en la base de datos.\n")
            }
        }
        return sb.toString()
    }

    fun mostrarDatosPrueba(): String {
        val db = this.readableDatabase
        try {
            return mostrarDatosPrueba(db)
        } finally {
            db.close()
        }
    }

    fun registrarUsuario(username: String, password: String): Boolean {
        // Mantener firma antigua para compatibilidad; sobrecarga con más campos disponible
        return registrarUsuario(username, password, null, null, null)
    }

    // Sobrecarga que permite añadir name/age/number
    fun registrarUsuario(username: String, password: String, name: String? = null, age: Int? = null, number: String? = null): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("username", username)
            put("password", password)
            if (!name.isNullOrEmpty()) put("name", name)
            if (age != null && age >= 0) put("age", age)
            if (!number.isNullOrEmpty()) put("number", number)
        }
        val result = db.insertWithOnConflict("usuarios", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        return result != -1L
    }

    fun iniciarSesion(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id FROM usuarios WHERE name = ? AND password = ?",
            arrayOf(username, password)
        )
        cursor.use {
            return it.count > 0
        }
    }

    // Obtener usuario por username (id, username, name, age, number)
    fun obtenerUsuarioPorUsername(username: String): Cursor {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT id, username, name, age, number FROM usuarios WHERE username = ?",
            arrayOf(username)
        )
    }

    fun mostrarHoteles(db: SQLiteDatabase): Cursor {
        return db.rawQuery("SELECT id, nombre, direccion, telefono, foto FROM HOTELES", null)
    }

    fun mostrarHabitacionesPorHotel(db: SQLiteDatabase, hotelId: Long): Cursor {
        return db.rawQuery(
            "SELECT id, numero_habitacion, tipo, precio, foto FROM habitaciones WHERE id_hotel = ?",
            arrayOf(hotelId.toString())
        )
    }

    fun mostrarReservasPorUsuario(db: SQLiteDatabase, userId: Long): Cursor {
        return db.rawQuery(
            "SELECT id, id_hotel, id_habitacion, nombre, fecha_entrada, fecha_salida, numero_habitacion FROM reservas WHERE id_usuario = ?",
            arrayOf(userId.toString())
        )
    }

    fun crearReserva(idHotel: Long, idHabitacion: Long, idUsuario: Long, nombre: String, fechaEntrada: String, fechaSalida: String, numeroHabitacion: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("id_hotel", idHotel)
            put("id_habitacion", idHabitacion)
            put("id_usuario", idUsuario)
            put("nombre", nombre)
            put("fecha_entrada", fechaEntrada)
            put("fecha_salida", fechaSalida)
            put("numero_habitacion", numeroHabitacion)
        }
        val result = db.insert("reservas", null, values)
        return result != -1L
    }

    fun cancelarReserva(reservaId: Long): Boolean {
        val db = this.writableDatabase
        val result = db.delete("reservas", "id = ?", arrayOf(reservaId.toString()))
        return result > 0
    }

    // Métodos para obtener información específica con fotos
    fun obtenerHotelConFoto(hotelId: Long): Cursor {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT id, nombre, direccion, telefono, foto FROM HOTELES WHERE id = ?",
            arrayOf(hotelId.toString())
        )
    }

    fun obtenerHabitacionConFoto(habitacionId: Long): Cursor {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT id, id_hotel, numero_habitacion, tipo, precio, foto FROM habitaciones WHERE id = ?",
            arrayOf(habitacionId.toString())
        )
    }

    fun normalizeFotoPathsInDb() {
        val db = this.writableDatabase
        try {
            db.beginTransaction()
            try {
                // Helper local (mismo que insertFromAssets)
                fun normalizeAssetPath(p: String?): String? {
                    if (p.isNullOrEmpty()) return null
                    var s = p.replace("\\", "/").trim()
                    s = s.removePrefix("file:///android_asset/")
                    while (s.startsWith("/")) s = s.removePrefix("/")
                    while (s.contains("images/images/")) s = s.replace("images/images/", "images/")
                    s = s.replace("./", "")
                    return s
                }

                // Actualizar HOTELES
                val c = db.rawQuery("SELECT id, foto FROM HOTELES", null)
                c.use {
                    if (it.moveToFirst()) {
                        do {
                            val id = it.getLong(0)
                            val foto = try { it.getString(1) } catch (_: Exception) { null }
                            val normalized = normalizeAssetPath(foto)
                            if (normalized != null && normalized != foto) {
                                val cv = ContentValues().apply { put("foto", normalized) }
                                db.update("HOTELES", cv, "id = ?", arrayOf(id.toString()))
                            }
                        } while (it.moveToNext())
                    }
                }

                // Actualizar habitaciones
                val rc = db.rawQuery("SELECT id, foto FROM habitaciones", null)
                rc.use {
                    if (it.moveToFirst()) {
                        do {
                            val id = it.getLong(0)
                            val foto = try { it.getString(1) } catch (_: Exception) { null }
                            val normalized = normalizeAssetPath(foto)
                            if (normalized != null && normalized != foto) {
                                val cv = ContentValues().apply { put("foto", normalized) }
                                db.update("habitaciones", cv, "id = ?", arrayOf(id.toString()))
                            }
                        } while (it.moveToNext())
                    }
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            try { db.close() } catch (_: Exception) {}
        }
    }

    // Nuevo: actualizar campos de usuario por username
    fun actualizarUsuarioPorUsername(username: String, name: String? = null, age: Int? = null, number: String? = null): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            if (name != null) put("name", name) else putNull("name")
            if (age != null && age >= 0) put("age", age) else putNull("age")
            if (number != null) put("number", number) else putNull("number")
        }
        val rows = try {
            db.update("usuarios", values, "username = ?", arrayOf(username))
        } catch (_: Exception) {
            0
        }
        return rows > 0
    }

    // Nuevo: actualizar credenciales (password) y/o número por username
    fun actualizarCredencialesPorUsername(username: String, password: String? = null, number: String? = null): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        if (password != null) values.put("password", password)
        if (number != null) values.put("number", number)

        // Si no se pasó nada para actualizar, devolver false
        if (values.size() == 0) return false

        val rows = try {
            db.update("usuarios", values, "username = ?", arrayOf(username))
        } catch (_: Exception) {
            0
        }
        return rows > 0
    }
}