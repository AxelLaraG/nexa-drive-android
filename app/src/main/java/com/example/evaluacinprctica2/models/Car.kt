package com.example.evaluacinprctica2.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.database.PropertyName

data class Car(
    @PropertyName("ID") var ID: String = "",
    @PropertyName("Estatus") var Estatus: String="",
    @PropertyName("Fecha_Alta") var Fecha_Alta: String = "",
    @PropertyName("Fecha_Renta") var Fecha_Renta: String = "",
    @PropertyName("FotoUrl") var FotoUrl: String = "",
    @PropertyName("Marca") var Marca: String = "",
    @PropertyName("Modelo") var Modelo: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(ID)
        parcel.writeString(Fecha_Alta)
        parcel.writeString(Fecha_Renta)
        parcel.writeString(FotoUrl)
        parcel.writeString(Marca)
        parcel.writeString(Modelo)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Car> {
        override fun createFromParcel(parcel: Parcel): Car {
            return Car(parcel)
        }

        override fun newArray(size: Int): Array<Car?> {
            return arrayOfNulls(size)
        }
    }
}

