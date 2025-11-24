package com.example.app_reservas_hotel.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.example.app_reservas_hotel.Login
import com.example.app_reservas_hotel.MiInformacionActivity
import com.example.app_reservas_hotel.VerReservasActivity
import com.example.app_reservas_hotel.HotelesActivity
import java.text.SimpleDateFormat
import java.util.*

object UiUtils {

    fun setupToolbar(activity: AppCompatActivity, toolbarResId: Int, showHomeAsUp: Boolean = false) {
        try {
            val toolbar = activity.findViewById<Toolbar>(toolbarResId)
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.setDisplayShowTitleEnabled(false)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(showHomeAsUp)
        } catch (_: Exception) {
        }
    }

    fun initDrawer(activity: Activity, drawerResId: Int, navViewResId: Int): Pair<DrawerLayout?, NavigationView?> {
        val drawer = try { activity.findViewById<DrawerLayout>(drawerResId) } catch (_: Exception) { null }
        val nav = try { activity.findViewById<NavigationView>(navViewResId) } catch (_: Exception) { null }
        try {
            if (nav != null && activity is NavigationView.OnNavigationItemSelectedListener) {
                nav.setNavigationItemSelectedListener(activity)
            }
        } catch (_: Exception) {
        }
        return Pair(drawer, nav)
    }

    fun bindBackButton(activity: Activity, backBtnResId: Int) {
        try {
            val btnBack = activity.findViewById<ImageButton>(backBtnResId)
            btnBack?.setOnClickListener { activity.finish() }
        } catch (_: Exception) {
        }
    }

    fun bindMenuButton(activity: Activity, menuBtnResId: Int, drawerLayout: DrawerLayout?) {
        try {
            val btnMenu = activity.findViewById<ImageButton>(menuBtnResId)
            btnMenu?.setOnClickListener {
                try {
                    drawerLayout?.openDrawer(androidx.core.view.GravityCompat.START)
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    fun getLoggedUsername(context: Context): String? {
        // Delegar en PrefsUtils
        return try {
            PrefsUtils.getLoggedUsername(context)
        } catch (_: Exception) { null }
    }

    fun styleSearchView(searchView: SearchView?, context: Context) {
        if (searchView == null) return
        try {
            val magIcon = searchView.findViewById<ImageView?>(androidx.appcompat.R.id.search_mag_icon)
            magIcon?.setImageResource(android.R.drawable.ic_menu_search)
            magIcon?.setColorFilter(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.black))

            val searchEditText = searchView.findViewById<android.widget.EditText?>(androidx.appcompat.R.id.search_src_text)
            searchEditText?.setTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.black))
            searchEditText?.setHintTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.black))

            val plate = searchView.findViewById<android.view.View?>(androidx.appcompat.R.id.search_plate)
            plate?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            searchView.setPadding(8, 0, 8, 0)
        } catch (_: Exception) {
        }
    }

    /**
     * Maneja la selección de un item del NavigationView de forma centralizada.
     * Devuelve true si fue manejado.
     */
    fun handleNavigationSelection(activity: Activity, itemId: Int, drawer: DrawerLayout?, username: String? = null): Boolean {
        try {
            when (itemId) {
                com.example.app_reservas_hotel.R.id.nav_hoteles -> {
                    val intent = Intent(activity, HotelesActivity::class.java)
                    activity.startActivity(intent)
                }
                com.example.app_reservas_hotel.R.id.nav_reservas -> {
                    val intent = Intent(activity, VerReservasActivity::class.java)
                    username?.let { intent.putExtra("username", it) }
                    activity.startActivity(intent)
                }
                com.example.app_reservas_hotel.R.id.nav_mi_info -> {
                    val intent = Intent(activity, MiInformacionActivity::class.java)
                    activity.startActivity(intent)
                }
                com.example.app_reservas_hotel.R.id.nav_logout -> {
                    // Delegar limpieza de sesión a PrefsUtils
                    try { PrefsUtils.clearLoggedUsername(activity) } catch (_: Exception) {}
                    val intent = Intent(activity, Login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                    if (activity is AppCompatActivity) {
                        activity.finish()
                    }
                }
                else -> return false
            }
            try { drawer?.closeDrawers() } catch (_: Exception) {}
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Adjunta el comportamiento de selector de rango (entrada/salida) a un CalendarView y tres TextViews.
     * onRangeChanged se invoca cada vez que cambia el rango con (entradaStr, salidaStr) en formato yyyy-MM-dd.
     */
    fun attachCalendarRangeSelector(
        context: Context,
        calendar: CalendarView,
        tvEntrada: TextView,
        tvSalida: TextView,
        tvHelp: TextView,
        initialEntradaMillis: Long,
        initialSalidaMillis: Long,
        onRangeChanged: (String, String) -> Unit
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Normalizar inicio si es <=0
        val calNow = Calendar.getInstance()
        calNow.set(Calendar.HOUR_OF_DAY, 0)
        calNow.set(Calendar.MINUTE, 0)
        calNow.set(Calendar.SECOND, 0)
        calNow.set(Calendar.MILLISECOND, 0)
        val todayMillis = calNow.timeInMillis

        var entradaMillis = if (initialEntradaMillis > 0) initialEntradaMillis else todayMillis
        var salidaMillis = if (initialSalidaMillis > 0) initialSalidaMillis else (entradaMillis + 24 * 60 * 60 * 1000)

        tvEntrada.text = context.getString(com.example.app_reservas_hotel.R.string.fecha_with_value, context.getString(com.example.app_reservas_hotel.R.string.btn_fecha_entrada), sdf.format(Date(entradaMillis)))
        tvSalida.text = context.getString(com.example.app_reservas_hotel.R.string.fecha_with_value, context.getString(com.example.app_reservas_hotel.R.string.btn_fecha_salida), sdf.format(Date(salidaMillis)))

        // Estilo inicial: Entrada seleccionada por defecto
        tvEntrada.setTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.primary))
        tvEntrada.setTypeface(null, android.graphics.Typeface.BOLD)
        tvSalida.setTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.on_surface))
        tvSalida.setTypeface(null, android.graphics.Typeface.NORMAL)
        tvHelp.setTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.secondary_text))

        calendar.minDate = todayMillis
        calendar.date = entradaMillis

        var selectingEntrada = true

        tvEntrada.setOnClickListener {
            selectingEntrada = true
            tvEntrada.setTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.primary))
            tvEntrada.setTypeface(null, android.graphics.Typeface.BOLD)
            tvSalida.setTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.on_surface))
            tvSalida.setTypeface(null, android.graphics.Typeface.NORMAL)
            tvHelp.setTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.secondary_text))
            tvHelp.text = context.getString(com.example.app_reservas_hotel.R.string.select_dates)
        }

        tvSalida.setOnClickListener {
            selectingEntrada = false
            tvSalida.setTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.primary))
            tvSalida.setTypeface(null, android.graphics.Typeface.BOLD)
            tvEntrada.setTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.on_surface))
            tvEntrada.setTypeface(null, android.graphics.Typeface.NORMAL)
            tvHelp.setTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.secondary_text))
            tvHelp.text = context.getString(com.example.app_reservas_hotel.R.string.select_dates)
        }

        calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            try {
                val selCal = Calendar.getInstance()
                selCal.set(Calendar.YEAR, year)
                selCal.set(Calendar.MONTH, month)
                selCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selCal.set(Calendar.HOUR_OF_DAY, 0)
                selCal.set(Calendar.MINUTE, 0)
                selCal.set(Calendar.SECOND, 0)
                selCal.set(Calendar.MILLISECOND, 0)
                val selMillis = selCal.timeInMillis

                if (selectingEntrada) {
                    entradaMillis = selMillis
                    if (entradaMillis >= salidaMillis) {
                        salidaMillis = entradaMillis + 24 * 60 * 60 * 1000
                        tvSalida.text = context.getString(com.example.app_reservas_hotel.R.string.fecha_with_value, context.getString(com.example.app_reservas_hotel.R.string.btn_fecha_salida), sdf.format(Date(salidaMillis)))
                    }
                    tvEntrada.text = context.getString(com.example.app_reservas_hotel.R.string.fecha_with_value, context.getString(com.example.app_reservas_hotel.R.string.btn_fecha_entrada), sdf.format(Date(entradaMillis)))
                } else {
                    salidaMillis = selMillis
                    if (salidaMillis <= entradaMillis) {
                        entradaMillis = salidaMillis - 24 * 60 * 60 * 1000
                        tvEntrada.text = context.getString(com.example.app_reservas_hotel.R.string.fecha_with_value, context.getString(com.example.app_reservas_hotel.R.string.btn_fecha_entrada), sdf.format(Date(entradaMillis)))
                    }
                    tvSalida.text = context.getString(com.example.app_reservas_hotel.R.string.fecha_with_value, context.getString(com.example.app_reservas_hotel.R.string.btn_fecha_salida), sdf.format(Date(salidaMillis)))
                }

                tvHelp.text = context.getString(com.example.app_reservas_hotel.R.string.rango_seleccionado, sdf.format(Date(entradaMillis)), sdf.format(Date(salidaMillis)))
                tvHelp.setTextColor(ContextCompat.getColor(context, com.example.app_reservas_hotel.R.color.highlight))

                onRangeChanged.invoke(sdf.format(Date(entradaMillis)), sdf.format(Date(salidaMillis)))
            } catch (_: Exception) {
            }
        }
    }
}
