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
        val tvModelo: TextView = view.findViewById(R.id.tvModelo)
        val tvMarca: TextView = view.findViewById(R.id.tvMarca)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val ivCarImage: ImageView = view.findViewById(R.id.ivCarImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val car = carList[position]
        holder.tvModelo.text = car.Modelo
        holder.tvMarca.text = car.Marca
        holder.tvEstado.text = car.Estatus

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

