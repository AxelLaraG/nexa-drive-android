package com.example.evaluacinprctica2.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.database.PropertyName

data class Rentas(
    @PropertyName("ID") var ID: String = "",
    @PropertyName("ID_Car") var ID_Car: String="",
    @PropertyName("Fecha_Renta") var Fecha_Renta: String = "",
    @PropertyName("Fecha_Dev") var Fecha_Dev: String ="",
    @PropertyName("ID_User") var ID_User: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString()?:""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(ID)
        parcel.writeString(ID_Car)
        parcel.writeString(Fecha_Renta)
        parcel.writeString(Fecha_Dev)
        parcel.writeString(ID_User)
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
