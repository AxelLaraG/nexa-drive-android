package com.example.evaluacinprctica2.ui.home

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.evaluacinprctica2.R
import com.example.evaluacinprctica2.models.Car
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EditCarFragment : Fragment() {

    private lateinit var car: Car
    private lateinit var spinnerEstatus: Spinner
    private lateinit var etFechaAlta: EditText
    private lateinit var etFechaRenta: EditText
    private lateinit var etMarca: EditText
    private lateinit var etModelo: EditText
    private lateinit var btnDelete: Button
    private lateinit var btnSave : Button

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    companion object {
        private const val ARG_CAR = "car"

        fun newInstance(car: Car): EditCarFragment {
            val fragment = EditCarFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARG_CAR, car)  // Pasa el objeto Car como Parcelable
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_edit_car, container, false)

        spinnerEstatus = rootView.findViewById(R.id.spinnerEstatus)
        etFechaAlta = rootView.findViewById(R.id.etFechaAlta)
        etFechaRenta = rootView.findViewById(R.id.etFechaRenta)
        etMarca = rootView.findViewById(R.id.etMarca)
        etModelo = rootView.findViewById(R.id.etModelo)
        btnDelete = rootView.findViewById(R.id.btnDelete)
        btnSave = rootView.findViewById(R.id.btnSave)

        // Recuperar el objeto Car del Bundle
        car = arguments?.getParcelable(ARG_CAR) ?: return rootView

        Log.d("EditCarFragment", "Datos del coche: $car")

        // Rellenar los campos con los valores actuales del vehículo
        etFechaAlta.setText(car.Fecha_Alta)
        etFechaRenta.setText(car.Fecha_Renta)
        etMarca.setText(car.Marca)
        etModelo.setText(car.Modelo)

        // Configurar Spinner para Estatus (Activo/Inactivo)
        val statusAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.estatus_options, // Esta es la lista de opciones en strings.xml
            android.R.layout.simple_spinner_item
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEstatus.adapter = statusAdapter

        // Establecer el estatus seleccionado
        val estatusPos = if (car.Estatus == "Activo") 0 else 1
        spinnerEstatus.setSelection(estatusPos)

        // Configuración del DatePicker para Fecha Alta
        etFechaAlta.setOnClickListener {
            showDatePickerDialog(etFechaAlta)
        }

        // Configuración del DatePicker para Fecha Renta
        etFechaRenta.setOnClickListener {
            showDatePickerDialog(etFechaRenta)
        }

        // Lógica para eliminar vehículo
        btnDelete.setOnClickListener {
            showDeleteDialog()
        }

        // Configurar el DatePicker para la fecha de alta
        etFechaAlta.setOnClickListener {
            showDatePickerDialog { year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                etFechaAlta.setText(dateFormat.format(calendar.time))
            }
        }

        // Configurar el DatePicker para la fecha de renta
        etFechaRenta.setOnClickListener {
            showDatePickerDialog { year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                etFechaRenta.setText(dateFormat.format(calendar.time))
            }
        }

        // Lógica para guardar cambios
        btnSave.setOnClickListener {
            saveChanges()
        }

        return rootView
    }

    private fun showDatePickerDialog(onDateSet: (Int, Int, Int) -> Unit) {
        DatePickerDialog(
            requireContext(),
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                onDateSet(year, month, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showDeleteDialog() {
        val options = arrayOf("Eliminar de manera lógica", "Eliminar definitivamente")
        AlertDialog.Builder(requireContext())
            .setTitle("¿Cómo quieres eliminar este vehículo?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> deleteCarLogical()  // Eliminar lógicamente
                    1 -> deleteCarPhysical() // Eliminar físicamente
                }
            }
            .show()
    }

    private fun deleteCarLogical() {
        val updatedCar = car.copy(Estatus = "Inactivo")
        updateCar(updatedCar)
    }

    private fun deleteCarPhysical() {
        val db = FirebaseFirestore.getInstance()
        db.collection("vehiculos")
            .document(car.ID)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Vehículo eliminado", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveChanges() {
        // Validar que los campos no estén vacíos
        val Estatus = spinnerEstatus.selectedItem.toString()
        val fechaAlta = etFechaAlta.text.toString()
        val fechaRenta = etFechaRenta.text.toString()
        val marca = etMarca.text.toString()
        val modelo = etModelo.text.toString()

        if (Estatus.isEmpty() || fechaAlta.isEmpty() || marca.isEmpty() || modelo.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear el objeto Car con los datos actualizados
        val updatedCar = car.copy(
            Estatus = Estatus,
            Fecha_Alta = fechaAlta,
            Fecha_Renta = fechaRenta,
            Marca = marca,
            Modelo = modelo
        )

        // Actualizar en Firebase
        updateCar(updatedCar)
    }

    private fun updateCar(updatedCar: Car) {
        val db = FirebaseFirestore.getInstance()
        db.collection("vehiculos")
            .document(updatedCar.ID)
            .set(updatedCar)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Vehículo actualizado", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // Volver al fragmento anterior
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val date = "$dayOfMonth/${month + 1}/$year"
                editText.setText(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
}


