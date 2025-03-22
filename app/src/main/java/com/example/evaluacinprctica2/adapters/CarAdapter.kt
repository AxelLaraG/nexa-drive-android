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
import com.example.evaluacinprctica2.models.Rentas

class CarAdapter(
    private var carList: List<Car>,
    private var rentasMap: Map<String, Rentas>, // Mapa de rentas asociadas a autos
    private val onItemClick: (Car) -> Unit
) : RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    class CarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvId: TextView = view.findViewById(R.id.tvID)
        val tvModelo: TextView = view.findViewById(R.id.tvModelo)
        val tvMarca: TextView = view.findViewById(R.id.tvMarca)
        val tvEstatus: TextView = view.findViewById(R.id.tvEstado)
        val ivCarImage: ImageView = view.findViewById(R.id.ivCarImage)
        val tvFechaAlta: TextView = view.findViewById(R.id.tvFechaAlta)
        val tvFechaRenta: TextView = view.findViewById(R.id.tvFechaRenta)
        val tvFechaDev: TextView = view.findViewById(R.id.tvFechaDev)
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

        // Obtener la renta asociada al auto
        val renta = rentasMap[car.ID]
        if (renta != null) {
            holder.tvFechaRenta.visibility = View.VISIBLE
            holder.tvFechaRenta.text = "Fecha Renta: ${renta.Fecha_Renta}"
            holder.tvFechaDev.visibility = View.VISIBLE
            holder.tvFechaDev.text = "Fecha Devolución: ${renta.Fecha_Dev}"
        } else {
            holder.tvFechaRenta.visibility = View.GONE
            holder.tvFechaDev.visibility = View.GONE
        }

        Glide.with(holder.itemView.context)
            .load(car.FotoUrl)
            .apply(RequestOptions().transform(RoundedCorners(40)))
            .into(holder.ivCarImage)

        holder.itemView.setOnClickListener {
            onItemClick(car)
        }
    }

    override fun getItemCount(): Int = carList.size

    fun updateList(newList: List<Car>, newRentasMap: Map<String, Rentas>) {
        carList = newList
        rentasMap = newRentasMap
        notifyDataSetChanged()
    }
}