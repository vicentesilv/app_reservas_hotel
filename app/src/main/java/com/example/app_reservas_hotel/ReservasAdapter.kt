package com.example.app_reservas_hotel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReservasAdapter : RecyclerView.Adapter<ReservasAdapter.VH>() {

    private val items = mutableListOf<Reserva>()

    fun submitList(list: List<Reserva>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_reserva, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHotelName: TextView = itemView.findViewById(R.id.tvHotelName)
        private val tvFechas: TextView = itemView.findViewById(R.id.tvFechasReserva)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoReserva)

        fun bind(r: Reserva) {
            // Mostrar el nombre del hotel y número de habitación como encabezado
            val header = if (r.hotelName.isNotEmpty()) "${r.hotelName} • Hab ${r.numeroHabitacion}" else "Hab ${r.numeroHabitacion}"
            tvHotelName.text = header
            tvFechas.text = "Entrada: ${r.fechaEntrada} — Salida: ${r.fechaSalida}"
            tvEstado.text = "Reservado por: ${r.nombre}"
        }
    }
}
