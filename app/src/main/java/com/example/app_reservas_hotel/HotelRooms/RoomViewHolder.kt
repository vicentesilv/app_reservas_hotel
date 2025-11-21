package com.example.app_reservas_hotel.HotelRooms

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app_reservas_hotel.R
import com.example.app_reservas_hotel.tranformTboxToString

class RoomViewHolder(view: View): RecyclerView.ViewHolder(view) {
    var ivRoom = view.findViewById<ImageView>(R.id.ivRoom)
    var tvNumber= view.findViewById<TextView>(R.id.tvNumber)
    var tvType= view.findViewById<TextView>(R.id.tvType)
    var tvPrice= view.findViewById<TextView>(R.id.tvPrice)
    var tvDescription= view.findViewById<TextView>(R.id.tvDescription)
    var tvCapacity= view.findViewById<TextView>(R.id.tvCapacity)



    fun render(roomModel: Room){
        tvNumber.text = roomModel.num.toString()
        tvType.text= roomModel.type
        Glide.with(ivRoom.context).load(roomModel.image).centerCrop().into(ivRoom)
        tvPrice.text= "${roomModel.price} USD"
        tvDescription.text= roomModel.description
        tvCapacity.text= roomModel.capacity.toString()
    }
}