package com.example.evaluacinprctica2.ui.home

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.evaluacinprctica2.R
import com.example.evaluacinprctica2.models.Car
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
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
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var imgCarPhoto: ImageView
    private lateinit var btnChangePhoto: Button
    private var newPhotoUri: Uri? = null
    private var currentPhotoUrl: String = ""
    private val REQUEST_IMAGE_PICK = 1001
    private var photoFile: File? = null
    private lateinit var progressBar: ProgressBar
    private val CAMERA_REQUEST_CODE = 2000

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
        car = arguments?.getParcelable("car") ?: throw IllegalArgumentException("Car object must be passed to fragment")

        spinnerEstatus = rootView.findViewById(R.id.spinnerEstatus)
        etFechaAlta = rootView.findViewById(R.id.etFechaAlta)
        etFechaRenta = rootView.findViewById(R.id.etFechaRenta)
        etMarca = rootView.findViewById(R.id.etMarca)
        etModelo = rootView.findViewById(R.id.etModelo)
        btnDelete = rootView.findViewById(R.id.btnDelete)
        btnSave = rootView.findViewById(R.id.btnSave)
        progressBar = rootView.findViewById(R.id.progressBar)
        imgCarPhoto = rootView.findViewById(R.id.imgCarPhoto)
        btnChangePhoto = rootView.findViewById(R.id.btnChangePhoto)
        currentPhotoUrl = car.FotoUrl

        Glide.with(this).load(currentPhotoUrl).into(imgCarPhoto)

        if (currentPhotoUrl.isNotEmpty()) {
            imgCarPhoto.visibility = View.VISIBLE // Hacer visible si ya tiene una foto
        } else {
            imgCarPhoto.visibility = View.GONE // De lo contrario, mantenerlo oculto
        }

        btnChangePhoto.setOnClickListener {
            showPhotoOptionsDialog()
        }

        etFechaAlta.setText(car.Fecha_Alta)
        etFechaRenta.setText(car.Fecha_Renta)
        etMarca.setText(car.Marca)
        etModelo.setText(car.Modelo)

        val statusAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.estatus_options,
            android.R.layout.simple_spinner_item
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEstatus.adapter = statusAdapter

        val estatusPos = if (car.Estatus == "Activo") 0 else 1
        spinnerEstatus.setSelection(estatusPos)

        etFechaAlta.setOnClickListener {
            showDatePickerDialog(etFechaAlta)
        }

        etFechaRenta.setOnClickListener {
            val fechaAltaString = etFechaAlta.text.toString()
            if (fechaAltaString.isEmpty()) {
                Toast.makeText(requireContext(), "Primero selecciona la fecha de alta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convierte la fecha de alta a un objeto Date
            val fechaAlta = dateFormat.parse(fechaAltaString)

            if (fechaAlta != null) {
                // Abre el DatePicker para la fecha de renta
                showDatePickerDialog(etFechaRenta, fechaAlta)
            } else {
                Toast.makeText(requireContext(), "Fecha de alta inválida", Toast.LENGTH_SHORT).show()
            }
        }

        btnDelete.setOnClickListener {
            showDeleteDialog()
        }

        etFechaAlta.setOnClickListener {
            showDatePickerDialog { year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                etFechaAlta.setText(dateFormat.format(calendar.time))
            }
        }

        btnSave.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled = false
            btnDelete.isEnabled = false
            saveChanges()
        }

        return rootView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK || requestCode == CAMERA_REQUEST_CODE) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    // Foto seleccionada desde la galería
                    newPhotoUri = data?.data
                    newPhotoUri?.let { uri ->
                        Glide.with(this).load(uri).into(imgCarPhoto) // Actualiza el ImageView
                        imgCarPhoto.visibility = View.VISIBLE
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    photoFile?.let { file ->
                        newPhotoUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider",
                            file
                        )
                        Glide.with(this)
                            .load(newPhotoUri)
                            .into(imgCarPhoto)
                        imgCarPhoto.visibility = View.VISIBLE
                    }
                }

            }
        }
    }

    private fun showDatePickerDialog(editText: EditText, fechaAlta: Date) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0) // Asegurarse de establecer la hora a 00:00:00
                }

                // Verificar si la fecha seleccionada es anterior a la fecha de alta
                if (selectedDate.before(Calendar.getInstance().apply { time = fechaAlta })) {
                    Toast.makeText(requireContext(), "La fecha de renta no puede ser antes de la fecha de alta", Toast.LENGTH_SHORT).show()
                } else {
                    val formattedDate = dateFormat.format(selectedDate.time)
                    editText.setText(formattedDate) // Actualizar el campo con la fecha seleccionada

                    // Verificar si la fecha de renta es menor o igual a la fecha actual
                    val currentDate = Calendar.getInstance()
                    if (selectedDate.before(currentDate) || selectedDate == currentDate) {
                        // Cambiar el estatus a "Inactivo"
                        spinnerEstatus.setSelection(1) // Suponiendo que "Inactivo" está en la posición 1 del Spinner
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Configurar la fecha mínima seleccionable (fecha de alta)
        val calendarFechaAlta = Calendar.getInstance().apply { time = fechaAlta }
        calendarFechaAlta.set(Calendar.HOUR_OF_DAY, 0)
        calendarFechaAlta.set(Calendar.MINUTE, 0)
        calendarFechaAlta.set(Calendar.SECOND, 0)
        calendarFechaAlta.set(Calendar.MILLISECOND, 0)

        datePickerDialog.datePicker.minDate = calendarFechaAlta.timeInMillis // Establecer la fecha mínima

        datePickerDialog.show()
    }

    private fun showPhotoOptionsDialog() {
        val options = arrayOf("Tomar Foto", "Seleccionar de la Galería")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Seleccionar fuente de foto")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> takePhoto() // Cámara
                1 -> pickImageFromGallery() // Galería
            }
        }
        builder.show()
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }


    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permiso de cámara no concedido", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear un archivo para guardar la foto
        val imageFile = File(requireContext().externalCacheDir, "${UUID.randomUUID()}.jpg")

        // Asignar la URI para la foto
        photoFile = imageFile // Asegúrate de asignar el archivo a photoFile

        newPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, newPhotoUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } else {
            showErrorDialog("No se encontró una aplicación de cámara.")
        }
    }


    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
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

        // Verificar si la foto fue cambiada
        if (newPhotoUri != null) {
            // Subir la nueva foto y obtener la URL
            uploadCarPhoto(updatedCar) { newPhotoUrl ->
                // Actualizar la URL de la foto en el objeto updatedCar
                val carWithNewPhoto = updatedCar.copy(FotoUrl = newPhotoUrl)

                // Actualizar los datos del vehículo en Firebase
                updateCar(carWithNewPhoto)
            }
        } else {
            // Si no se cambió la foto, simplemente actualizamos el vehículo sin la foto
            updateCar(updatedCar)
        }
    }

    private fun uploadCarPhoto(car: Car, callback: (String) -> Unit) {
        // Lógica para subir la foto a Firebase Storage
        val storageReference = FirebaseStorage.getInstance().reference.child("car_photos/${car.ID}.jpg")

        storageReference.putFile(newPhotoUri!!)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val photoUrl = uri.toString()
                    callback(photoUrl) // Llamamos al callback con la nueva URL
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al subir la foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
                val date = "$year-${month + 1}-$dayOfMonth"
                editText.setText(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
}