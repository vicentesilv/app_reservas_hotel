package com.example.app_reservas_hotel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReservasAdapter : RecyclerView.Adapter<ReservasAdapter.VH>() {

    private val items = mutableListOf<Reserva>()

    // Callbacks que la Activity puede asignar
    var onEdit: ((Reserva) -> Unit)? = null
    var onDelete: ((Reserva) -> Unit)? = null

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
        private val btnEdit: View? = itemView.findViewById(R.id.btnEditReserva)
        private val btnDelete: View? = itemView.findViewById(R.id.btnDeleteReserva)

        fun bind(r: Reserva) {
            // Mostrar el nombre del hotel y número de habitación como encabezado
            val header = if (r.hotelName.isNotEmpty()) "${r.hotelName} • Hab ${r.numeroHabitacion}" else "Hab ${r.numeroHabitacion}"
            tvHotelName.text = header
            tvFechas.text = "Entrada: ${r.fechaEntrada} — Salida: ${r.fechaSalida}"
            tvEstado.text = "Reservado por: ${r.nombre}"

            // Asignar listeners; usamos getTag para recuperar la reserva desde la view cuando la Activity quiera manejar
            itemView.tag = r
            btnEdit?.setOnClickListener {
                // Propagar evento hacia el adapter (la Activity debe asignar onEdit)
                val parent = itemView.parent
                // búsqueda del adapter desde el contexto: subimos hasta encontrar RecyclerView y adaptador
                var adapter: ReservasAdapter? = null
                try {
                    adapter = (itemView.parent as? androidx.recyclerview.widget.RecyclerView)?.adapter as? ReservasAdapter
                } catch (_: Exception) {}
                if (adapter != null) adapter.onEdit?.invoke(r)
            }

            btnDelete?.setOnClickListener {
                var adapter: ReservasAdapter? = null
                try {
                    adapter = (itemView.parent as? androidx.recyclerview.widget.RecyclerView)?.adapter as? ReservasAdapter
                } catch (_: Exception) {}
                if (adapter != null) adapter.onDelete?.invoke(r)
            }
        }
    }
}
