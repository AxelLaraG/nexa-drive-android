package com.example.evaluacinprctica2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.evaluacinprctica2.R
import com.example.evaluacinprctica2.models.Car

class CarAdapter(private var carList: List<Car>) :
    RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    class CarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvId : TextView = view.findViewById(R.id.tvID)
        val tvModelo: TextView = view.findViewById(R.id.tvModelo)
        val tvMarca: TextView = view.findViewById(R.id.tvMarca)
        val tvEstatus: TextView = view.findViewById(R.id.tvEstado)
        val ivCarImage: ImageView = view.findViewById(R.id.ivCarImage)
        val tvFechaAlta : TextView = view.findViewById(R.id.tvFechaAlta)
        val tvFechaRenta : TextView = view.findViewById(R.id.tvFechaRenta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val car = carList[position]

        holder.tvId.text = "ID: ${car.ID}"
        holder.tvModelo.text = "Modelo: ${car.Modelo}"
        holder.tvMarca.text = "Marca: ${car.Marca}"
        holder.tvEstatus.text = "Estatus: ${car.Estatus}"
        holder.tvFechaAlta.text = "Fecha Alta: ${car.Fecha_Alta}"

        if (car.Fecha_Renta.isEmpty()) {
            holder.tvFechaRenta.visibility = View.GONE // Ocultar si la fecha está vacía
        } else {
            holder.tvFechaRenta.visibility = View.VISIBLE // Mostrar si tiene fecha
            holder.tvFechaRenta.text = "Fecha Renta: ${car.Fecha_Renta}"
        }

        Glide.with(holder.itemView.context)
            .load(car.FotoUrl)
            .apply(RequestOptions().transform(RoundedCorners(40)))
            .into(holder.ivCarImage)
    }

    override fun getItemCount(): Int = carList.size

    fun updateList(newList: List<Car>) {
        carList = newList
        notifyDataSetChanged()
    }
}

