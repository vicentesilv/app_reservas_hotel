package com.example.app_reservas_hotel

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, dataBaseName, null, databaseVersion) {
    companion object {
        private const val dataBaseName = "hotel_reservas.db"
        // Incrementar versión para aplicar nueva columna `stock` en habitaciones
        private const val databaseVersion = 7
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
                """
                .trimIndent()
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
                """
                .trimIndent()
        )
        db?.execSQL(
            """
                CREATE TABLE IF NOT EXISTS habitaciones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_hotel INTEGER NOT NULL,
                    numero_habitacion INTEGER NOT NULL,
                    stock INTEGER NOT NULL DEFAULT 1,
                    tipo TEXT NOT NULL,
                    precio REAL NOT NULL, -- Changed to REAL for Double
                    foto TEXT,
                    capacidad INTEGER,
                    descripcion TEXT
                );
                """
                .trimIndent()
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
                """
                .trimIndent()
        )

        // Intentar poblar la base desde assets/data.json solo si no hay hoteles
        db?.let {
            try {
                if (!hasData(it, "HOTELES")) {
                    insertFromAssets(it)
                }
            } catch (_: Exception) {
                // Silenciar fallos de inserción automática para no romper onCreate
            }
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db == null) return

        try {
            // Migración incremental: si venimos de versión menor que 7, intentamos añadir columna `stock`
            if (oldVersion < 7) {
                try {
                    db.execSQL("ALTER TABLE habitaciones ADD COLUMN stock INTEGER NOT NULL DEFAULT 1")
                    // Otras migraciones entre versiones podrían añadirse aquí
                    return
                } catch (e: Exception) {
                    Log.w("DatabaseHelper", "onUpgrade: fallo al añadir columna stock, fallback a recrear tablas", e)
                    // fallback: eliminar y recrear
                }
            }

            // Fallback genérico: recrear todas las tablas
            db.execSQL("DROP TABLE IF EXISTS reservas")
            db.execSQL("DROP TABLE IF EXISTS habitaciones")
            db.execSQL("DROP TABLE IF EXISTS HOTELES")
            db.execSQL("DROP TABLE IF EXISTS usuarios")
            onCreate(db)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "onUpgrade: error al actualizar la base de datos", e)
            try {
                // Intentar fallback definitivo
                db.execSQL("DROP TABLE IF EXISTS reservas")
                db.execSQL("DROP TABLE IF EXISTS habitaciones")
                db.execSQL("DROP TABLE IF EXISTS HOTELES")
                db.execSQL("DROP TABLE IF EXISTS usuarios")
                onCreate(db)
            } catch (_: Exception) {}
        }
    }

    // Manejar downgrades: por defecto SQLite lanza una excepción si la versión es menor.
    // Aquí aplicamos la misma lógica que en onUpgrade para recrear la base y evitar el crash.
    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
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
        Log.d("DatabaseHelper", "insertFromAssets: starting import from assets/data.json")
        val jsonString: String = try {
            context.assets.open("data.json").bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e("DatabaseHelper", "insertFromAssets: cannot open data.json", e)
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
            var hotelsInserted = 0
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
                    if (hotelId == -1L) {
                        Log.w("DatabaseHelper", "insertFromAssets: failed to insert hotel $nombre")
                        continue
                    }
                    hotelsInserted++
                    insertedAny = true

                    val habitacionesArray = hObj.optJSONArray("habitaciones") ?: JSONArray()
                    for (j in 0 until habitacionesArray.length()) {
                        val rObj = habitacionesArray.optJSONObject(j) ?: continue
                        val numero = rObj.optInt("numero_habitacion", j + 1)
                        val stock = rObj.optInt("stock", 1)
                        val tipo = rObj.optString("tipo", "Estándar")
                        val precio = rObj.optDouble("precio", 50.0) // Read as Double
                        val capacidad = rObj.optInt("capacidad")
                        val descripcion = rObj.optString("descripcion")
                        val fotoHabitacionRaw = rObj.optString("foto", "")
                        val fotoHabitacion = normalizeAssetPath(fotoHabitacionRaw)

                        val rv = ContentValues().apply {
                            put("id_hotel", hotelId)
                            put("numero_habitacion", numero)
                            put("stock", stock)
                            put("tipo", tipo)
                            put("precio", precio)
                            put("capacidad", capacidad)
                            put("descripcion", descripcion)
                            if (!fotoHabitacion.isNullOrEmpty()) {
                                put("foto", fotoHabitacion)
                            }
                        }
                        val rid = db.insert("habitaciones", null, rv)
                        if (rid == -1L) {
                            Log.w("DatabaseHelper", "insertFromAssets: failed to insert habitacion #$numero for hotelId=$hotelId")
                        }
                    }
                }

                // Insertar usuarios
                var usersInserted = 0
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
                    val ures = db.insertWithOnConflict("usuarios", null, uv, SQLiteDatabase.CONFLICT_IGNORE)
                    if (ures != -1L) usersInserted++
                    insertedAny = insertedAny || (ures != -1L)
                }

                db.setTransactionSuccessful()
                Log.d("DatabaseHelper", "insertFromAssets: transaction successful - hotelsInserted=$hotelsInserted usersInserted=$usersInserted")
            } finally {
                db.endTransaction()
            }

            return insertedAny
        } catch (e: JSONException) {
            Log.e("DatabaseHelper", "insertFromAssets: JSON error", e)
            return false
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "insertFromAssets: unexpected error", e)
            return false
        }
    }

    // Public helper to trigger import from assets (useful for debugging)
    fun importFromAssetFile(): Boolean {
        val db = this.writableDatabase
        try {
            val ok = insertFromAssets(db)
            Log.d("DatabaseHelper", "importFromAssetFile: result=$ok")
            return ok
        } finally {
            try { db.close() } catch (_: Exception) {}
        }
    }

    @Suppress("unused")
    private fun hasData(db: SQLiteDatabase, table: String): Boolean {
        val cursor: Cursor = db.rawQuery("SELECT COUNT(*) FROM $table", null)
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) > 0 else false
        }
    }

    @Suppress("unused")
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

    @Suppress("unused")
    fun mostrarDatosPrueba(): String {
        val db = this.readableDatabase
        try {
            return mostrarDatosPrueba(db)
        } finally {
            db.close()
        }
    }

    @Suppress("unused")
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
        try {
            val cursor = db.rawQuery(
                "SELECT id FROM usuarios WHERE username = ? AND password = ?",
                arrayOf(username, password)
            )
            cursor.use {
                return it.count > 0
            }
        } finally {
            db.close()
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
            "SELECT id, numero_habitacion, tipo, precio, foto, capacidad, descripcion FROM habitaciones WHERE id_hotel = ?",
            arrayOf(hotelId.toString())
        )
    }

    @Suppress("unused")
    fun mostrarReservasPorUsuario(db: SQLiteDatabase, userId: Long): Cursor {
        return db.rawQuery(
            "SELECT id, id_hotel, id_habitacion, nombre, fecha_entrada, fecha_salida, numero_habitacion FROM reservas WHERE id_usuario = ?",
            arrayOf(userId.toString())
        )
    }

    fun crearReserva(idHotel: Long, idHabitacion: Long, idUsuario: Long, nombre: String, fechaEntrada: String, fechaSalida: String, numeroHabitacion: Int): Boolean {
        val db = this.writableDatabase
        try {
            db.beginTransaction()
            // Insertar reserva
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
            if (result == -1L) {
                // fallo al insertar reserva
                return false
            }

            // Reducir numero_habitacion en la tabla habitaciones para la fila correspondiente
            try {
                // Usar una única sentencia UPDATE atómica para decrementar solo si numero_habitacion > 0
                val updated = db.compileStatement("UPDATE habitaciones SET numero_habitacion = numero_habitacion - 1 WHERE id = ? AND numero_habitacion > 0").apply {
                    bindLong(1, idHabitacion)
                }.executeUpdateDelete()

                if (updated <= 0) {
                    // 0 filas afectadas: o la habitación no existe, o numero_habitacion ya era 0
                    // Comprobar existencia de la habitación para distinguir el caso
                    val existsCur = db.rawQuery("SELECT COUNT(*) FROM habitaciones WHERE id = ?", arrayOf(idHabitacion.toString()))
                    var exists = false
                    existsCur.use {
                        if (it.moveToFirst()) {
                            try { exists = it.getInt(0) > 0 } catch (_: Exception) { exists = false }
                        }
                    }
                    if (!exists) {
                        Log.e("DatabaseHelper", "crearReserva: no se encontró la habitación id=$idHabitacion")
                        return false
                    } else {
                        // habitación existe pero numero_habitacion ya era 0 -> no hay disponibilidad
                        Log.e("DatabaseHelper", "crearReserva: habitación id=$idHabitacion sin disponibilidad (numero_habitacion==0)")
                        return false
                    }
                }
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "crearReserva: error al reducir numero_habitacion", e)
                return false
            }

            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "crearReserva transaction failed", e)
            return false
        } finally {
            try { db.endTransaction() } catch (_: Exception) {}
            try { db.close() } catch (_: Exception) {}
            // en caso de fallo devolverá false
        }
    }

    @Suppress("unused")
    fun cancelarReserva(reservaId: Long): Boolean {
        val db = this.writableDatabase
        val result = db.delete("reservas", "id = ?", arrayOf(reservaId.toString()))
        return result > 0
    }

    // Métodos para obtener información específica con fotos
    @Suppress("unused")
    fun obtenerHotelConFoto(hotelId: Long): Cursor {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT id, nombre, direccion, telefono, foto FROM HOTELES WHERE id = ?",
            arrayOf(hotelId.toString())
        )
    }

    @Suppress("unused")
    fun obtenerHabitacionConFoto(habitacionId: Long): Cursor {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT id, id_hotel, numero_habitacion, tipo, precio, foto FROM habitaciones WHERE id = ?",
            arrayOf(habitacionId.toString())
        )
    }

    @Suppress("unused")
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
    @Suppress("unused")
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

    @Suppress("unused")
    fun insertSampleDataIfEmpty() {
        val db = this.writableDatabase
        try {
            // Comprueba si ya hay datos
            val c = db.rawQuery("SELECT COUNT(*) FROM HOTELES", null)
            var hotelsCount = 0
            c.use {
                if (it.moveToFirst()) hotelsCount = it.getInt(0)
            }

            if (hotelsCount > 0) return // ya tiene datos

            db.beginTransaction()
            try {
                // Insertar 2 hoteles de ejemplo
                val hv1 = ContentValues().apply {
                    put("nombre", "Hotel Sol y Mar")
                    put("direccion", "Av. del Mar 123")
                    put("telefono", "600-111-222")
                    putNull("foto")
                }
                val hid1 = db.insert("HOTELES", null, hv1)

                val hv2 = ContentValues().apply {
                    put("nombre", "Hotel La Rivera")
                    put("direccion", "Calle Rivera 45")
                    put("telefono", "600-222-333")
                    putNull("foto")
                }
                val hid2 = db.insert("HOTELES", null, hv2)

                // Habitaciones para hotel 1
                val r1 = ContentValues().apply {
                    put("id_hotel", hid1)
                    put("numero_habitacion", 1)
                    put("stock", 1)
                    put("tipo", "Individual")
                    put("precio", 60.0)
                    put("capacidad", 1)
                    put("descripcion", "Habitacion Individual perfecta para vacaciones o viajes de negocios.")
                }
                db.insert("habitaciones", null, r1)

                val r2 = ContentValues().apply {
                    put("id_hotel", hid1)
                    put("numero_habitacion", 2)
                    put("stock", 1)
                    put("tipo", "Doble")
                    put("precio", 70.0)
                    put("capacidad", 2)
                    put("descripcion", "Habitacion Doble perfecta para familias")
                }
                db.insert("habitaciones", null, r2)

                // Habitaciones para hotel 2
                val r3 = ContentValues().apply {
                    put("id_hotel", hid2)
                    put("numero_habitacion", 1)
                    put("stock", 1)
                    put("tipo", "Suite")
                    put("precio", 90.0)
                    put("capacidad", 4)
                    put("descripcion", "Habitacion Suite perfecta para vacaciones de lujo")
                }
                db.insert("habitaciones", null, r3)

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            try { db.close() } catch (_: Exception) {}
        }
    }
}
