package com.example.evaluacinprctica2.ui.home

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isEmpty
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.evaluacinprctica2.R
import com.example.evaluacinprctica2.models.Car
import com.example.evaluacinprctica2.models.Rentas
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EditCarFragment : Fragment() {

    private lateinit var car: Car

    private lateinit var spinnerEstatus: Spinner
    private lateinit var spinnerUsr: Spinner
    private lateinit var spinnerRentas: Spinner

    private lateinit var etFechaAlta: EditText
    private lateinit var etFechaRenta: EditText
    private lateinit var etFechaDev: EditText
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
            bundle.putParcelable(ARG_CAR, car)
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
        spinnerUsr = rootView.findViewById(R.id.spinnerUsr)
        spinnerRentas = rootView.findViewById(R.id.spinnerRentas)

        etFechaAlta = rootView.findViewById(R.id.etFechaAlta)
        etFechaRenta = rootView.findViewById(R.id.etFechaRenta)
        etFechaDev = rootView.findViewById(R.id.etFechaDev)

        etMarca = rootView.findViewById(R.id.etMarca)
        etModelo = rootView.findViewById(R.id.etModelo)

        btnDelete = rootView.findViewById(R.id.btnDelete)
        btnSave = rootView.findViewById(R.id.btnSave)
        progressBar = rootView.findViewById(R.id.progressBar)
        imgCarPhoto = rootView.findViewById(R.id.imgCarPhoto)
        btnChangePhoto = rootView.findViewById(R.id.btnChangePhoto)
        currentPhotoUrl = car.FotoUrl

        Glide.with(this).load(currentPhotoUrl).into(imgCarPhoto)

        cargarElementos()
        selector()

        btnChangePhoto.setOnClickListener {
            showPhotoOptionsDialog()
        }

        etFechaAlta.setOnClickListener {
            showDatePickerDialog { year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                etFechaAlta.setText(dateFormat.format(calendar.time))
            }
        }

        btnDelete.setOnClickListener {
            showDeleteDialog()
        }

        btnSave.setOnClickListener {

            saveChanges()
        }

        return rootView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK || requestCode == CAMERA_REQUEST_CODE) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
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

    private fun selector() {
        spinnerRentas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = spinnerRentas.selectedItem?.toString()

                if (selectedItem == "Nueva Renta") {
                    etFechaRenta.visibility = View.VISIBLE
                    etFechaDev.visibility = View.VISIBLE
                    spinnerUsr.visibility = View.VISIBLE

                    etFechaDev.setText("")
                    etFechaRenta.setText("")

                    cargarUsuarios()  // Asegurar que los usuarios están cargados
                    eventosRentas()
                } else if (selectedItem == "Seleccione una opción") {
                    etFechaRenta.visibility = View.GONE
                    etFechaDev.visibility = View.GONE
                    spinnerUsr.visibility = View.GONE
                } else {
                    etFechaRenta.visibility = View.VISIBLE
                    etFechaDev.visibility = View.VISIBLE
                    spinnerUsr.visibility = View.VISIBLE

                    cargarUsuarios()  // Asegurar que el spinner tiene datos antes de seleccionarlo
                    obtenerDatosRentaSeleccionada()
                    eventosRentas()
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // No hacer nada si no se selecciona nada
            }
        }
    }

    private fun obtenerDatosRentaSeleccionada() {
        val rentaId = obtenerIDRentaSeleccionada()
        if (rentaId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No se encontró la ID de la renta", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("Rents").document(rentaId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fechaRenta = document.getString("Fecha_Renta") ?: ""
                    val fechaDevolucion = document.getString("Fecha_Dev") ?: ""
                    val usuarioId = document.getString("ID_User") ?: ""

                    // Actualizar los campos en la vista
                    etFechaRenta.setText(fechaRenta)
                    etFechaDev.setText(fechaDevolucion)

                    if (spinnerUsr.adapter == null) {
                        return@addOnSuccessListener
                    }

                    val adapter = spinnerUsr.adapter as ArrayAdapter<String>
                    val position = adapter.getPosition(usuarioId)

                    if (position >= 0) {
                        spinnerUsr.setSelection(position)
                    }
                }
            }
    }

    private fun eventosRentas(){
        etFechaRenta.setOnClickListener {
            val fechaAltaString = etFechaAlta.text.toString()
            if (fechaAltaString.isEmpty()) {
                Toast.makeText(requireContext(), "Primero selecciona la fecha de alta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fechaAlta = dateFormat.parse(fechaAltaString)

            if (fechaAlta != null) {
                showDatePickerDialog(etFechaRenta, fechaAlta)
            } else {
                Toast.makeText(requireContext(), "Fecha de alta inválida", Toast.LENGTH_SHORT).show()
            }
        }

        etFechaDev.setOnClickListener{
            val fechaRentaString = etFechaRenta.text.toString()
            if (fechaRentaString.isEmpty()){
                Toast.makeText(requireContext(),"Primero selecciona la fecha de renta",Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val fechaRenta = dateFormat.parse(fechaRentaString)

            if (fechaRenta != null){
                showDatePickerDev(etFechaDev,fechaRenta)
            } else {
                Toast.makeText(requireContext(), "Fecha de alta inválida", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarElementos(){

        if (currentPhotoUrl.isNotEmpty()){
            imgCarPhoto.visibility = View.VISIBLE
        }else{
            imgCarPhoto.visibility = View.GONE
        }

        etFechaAlta.setText(car.Fecha_Alta)
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

        validarRentas()

    }

    private fun validarRentas() {
        val db = FirebaseFirestore.getInstance()

        db.collection("Rents")
            .whereEqualTo("ID_Auto", car.ID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val rentasList = mutableListOf<String>()
                val rentasMap = mutableMapOf<String, String>() // Mapa para asociar usuario con ID_Renta

                rentasList.add("Seleccione una opción")
                rentasList.add("Nueva Renta")

                for (doc in querySnapshot.documents) {
                    val rent = doc.toObject(Rentas::class.java)
                    val usuario = rent?.ID_User
                    val rentaId = doc.id  // ID de la renta (documento de Firestore)

                    if (usuario != null) {
                        rentasList.add(usuario)
                        rentasMap[usuario] = rentaId  // Asociamos el nombre de usuario con el ID de la renta
                    }
                }

                val rentasAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    rentasList
                )

                rentasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerRentas.adapter = rentasAdapter

                // Guardamos el mapa de ID de rentas
                spinnerRentas.tag = rentasMap
            }
    }

    private fun obtenerIDRentaSeleccionada(): String? {
        val rentasMap = spinnerRentas.tag as? Map<String, String>
        val usuarioSeleccionado = spinnerRentas.selectedItem.toString()
        return rentasMap?.get(usuarioSeleccionado)
    }

    private fun cargarUsuarios(){
        val db = FirebaseFirestore.getInstance()

        db.collection("usuarios") // Suponiendo que "Users" es tu colección de usuarios
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val usuariosList = mutableListOf<String>()

                    usuariosList.add("Seleccione un usuario")

                    // Recorremos los documentos de usuarios
                    for (document in querySnapshot.documents) {
                        val usuario = document.getString("nombre")  // Cambia esto según el campo de nombre del usuario en tu base de datos

                        if (usuario != null) {
                            usuariosList.add(usuario)  // Agregamos el nombre del usuario a la lista
                        }
                    }

                    // Configuración del adaptador para el Spinner
                    val usuariosAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        usuariosList
                    )

                    usuariosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerUsr.adapter = usuariosAdapter
                }
            }
    }

    private fun showDatePickerDev(editText: EditText, fechaRenta: Date) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                }

                if (selectedDate.before(Calendar.getInstance().apply { time = fechaRenta })) {
                    Toast.makeText(requireContext(), "La fecha de devolución no puede ser antes de la fecha de renta", Toast.LENGTH_SHORT).show()
                } else {
                    val formattedDate = dateFormat.format(selectedDate.time)
                    editText.setText(formattedDate)

                    val currentDate = Calendar.getInstance()
                    if (selectedDate.before(currentDate) || selectedDate == currentDate) {
                        spinnerEstatus.setSelection(0)
                    }
                    if (selectedDate.after(currentDate) && (currentDate.before(Calendar.getInstance().apply { time = fechaRenta }))){
                        spinnerEstatus.setSelection(1)
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        val calendarFechaAlta = Calendar.getInstance().apply { time = fechaRenta }
        calendarFechaAlta.set(Calendar.HOUR_OF_DAY, 0)
        calendarFechaAlta.set(Calendar.MINUTE, 0)
        calendarFechaAlta.set(Calendar.SECOND, 0)
        calendarFechaAlta.set(Calendar.MILLISECOND, 0)

        datePickerDialog.datePicker.minDate = calendarFechaAlta.timeInMillis // Establecer la fecha mínima

        datePickerDialog.show()
    }

    private fun showDatePickerDialog(editText: EditText, fechaAlta: Date) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                }

                if (selectedDate.before(Calendar.getInstance().apply { time = fechaAlta })) {
                    Toast.makeText(requireContext(), "La fecha de renta no puede ser antes de la fecha de alta", Toast.LENGTH_SHORT).show()
                } else {
                    val formattedDate = dateFormat.format(selectedDate.time)
                    editText.setText(formattedDate)

                    val currentDate = Calendar.getInstance()
                    if (selectedDate.before(currentDate) || selectedDate == currentDate) {
                        spinnerEstatus.setSelection(1)
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        val calendarFechaAlta = Calendar.getInstance().apply { time = fechaAlta }
        calendarFechaAlta.set(Calendar.HOUR_OF_DAY, 0)
        calendarFechaAlta.set(Calendar.MINUTE, 0)
        calendarFechaAlta.set(Calendar.SECOND, 0)
        calendarFechaAlta.set(Calendar.MILLISECOND, 0)

        datePickerDialog.datePicker.minDate = calendarFechaAlta.timeInMillis // Establecer la fecha mínima

        datePickerDialog.show()
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

    private fun showPhotoOptionsDialog() {
        val options = arrayOf("Tomar Foto", "Seleccionar de la Galería")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Seleccionar fuente de foto")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> takePhoto()
                1 -> pickImageFromGallery()
            }
        }
        builder.show()
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permiso de cámara no concedido", Toast.LENGTH_SHORT).show()
            return
        }

        val imageFile = File(requireContext().externalCacheDir, "${UUID.randomUUID()}.jpg")

        photoFile = imageFile

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

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showDeleteDialog() {
        val options = arrayOf("Eliminar de manera lógica", "Eliminar definitivamente")
        AlertDialog.Builder(requireContext())
            .setTitle("¿Cómo quieres eliminar este vehículo?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> deleteCarLogical()
                    1 -> deleteCarPhysical()
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

    private fun saveChanges(){
        val estatus = spinnerEstatus.selectedItem.toString()
        val fechaAlta = etFechaAlta.text.toString()
        val marca = etMarca.text.toString()
        val modelo = etModelo.text.toString()

        if (estatus.isEmpty() || fechaAlta.isEmpty() || marca.isEmpty() || modelo.isEmpty()){
            Toast.makeText(requireContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (spinnerRentas.selectedItem.toString() == "Nueva Renta"){
            if (etFechaRenta.text.toString().isEmpty() || etFechaDev.text.toString().isEmpty() || spinnerUsr.selectedItem.toString().isEmpty() || spinnerUsr.selectedItem.toString() == "Seleccione un usuario"){
                Toast.makeText(requireContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return
            }
            createRenta()
        } else if (spinnerRentas.selectedItem.toString() != "Seleccione una opción"){
            if (etFechaRenta.text.toString().isEmpty() || etFechaDev.text.toString().isEmpty() || spinnerUsr.selectedItem.toString().isEmpty() || spinnerUsr.selectedItem.toString() == "Seleccione un usuario"){
                Toast.makeText(requireContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return
            }
            updateRenta(obtenerIDRentaSeleccionada().toString())
        }

        btnSave.isEnabled = false
        btnDelete.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val updatedCar = car.copy(
            Estatus = estatus,
            Fecha_Alta = fechaAlta,
            Marca = marca,
            Modelo = modelo
        )

        if (newPhotoUri != null) {
            uploadCarPhoto(updatedCar) { newPhotoUrl ->
                val carWithNewPhoto = updatedCar.copy(FotoUrl = newPhotoUrl)
                updateCar(carWithNewPhoto)
            }
        } else {
            updateCar(updatedCar)
        }
    }

    private fun createRenta() {
        val db = FirebaseFirestore.getInstance()

        val fechaRenta = etFechaRenta.text.toString()
        val fechaDevolucion = etFechaDev.text.toString()
        val usuarioId = spinnerUsr.selectedItem.toString()

        val nuevaRenta = hashMapOf(
            "ID_Auto" to car.ID,
            "ID_User" to usuarioId,
            "Fecha_Renta" to fechaRenta,
            "Fecha_Devolucion" to fechaDevolucion
        )

        db.collection("Rents")
            .add(nuevaRenta)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(requireContext(), "Renta creada con éxito", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al crear la renta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRenta(rentaId: String) {
        val db = FirebaseFirestore.getInstance()

        // Obtenemos los datos a actualizar
        val fechaRenta = etFechaRenta.text.toString().trim()
        val fechaDevolucion = etFechaDev.text.toString().trim()
        val usuarioId = spinnerUsr.selectedItem?.toString()

        // Validar que los campos no estén vacíos
        if (fechaRenta.isEmpty() || fechaDevolucion.isEmpty() || usuarioId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        // Mapa con los datos a actualizar en Firestore
        val rentaActualizada = hashMapOf(
            "Fecha_Renta" to fechaRenta,
            "Fecha_Dev" to fechaDevolucion,
            "ID_User" to usuarioId
        )

        // Actualizar solo los campos especificados en Firestore
        db.collection("Rents")
            .document(rentaId)
            .update(rentaActualizada as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Renta actualizada con éxito", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al actualizar la renta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadCarPhoto(car: Car, callback: (String) -> Unit) {
        val storageReference = FirebaseStorage.getInstance().reference.child("car_photos/${car.ID}.jpg")

        storageReference.putFile(newPhotoUri!!)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val photoUrl = uri.toString()
                    callback(photoUrl)
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
}