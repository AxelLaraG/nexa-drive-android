package com.example.evaluacinprctica2.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.evaluacinprctica2.R
import com.example.evaluacinprctica2.databinding.FragmentAddCarBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddCarFragment : Fragment() {

    private var _binding: FragmentAddCarBinding? = null
    private val binding get() = _binding!!
    private var imageUri: Uri? = null

    private val CAMERA_REQUEST_CODE = 2000
    private val GALLERY_REQUEST_CODE = 1000

    // Referencia al ProgressBar
    private lateinit var progressBar: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa el ProgressBar
        progressBar = binding.root.findViewById(R.id.progressBar)

        // Botón para seleccionar imagen
        binding.btnSeleccionarFoto.setOnClickListener {
            showImageSourceDialog()
        }

        // Botón para guardar datos
        binding.btnGuardar.setOnClickListener {
            guardarVehiculo()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Tomar Foto con la Cámara", "Seleccionar Foto de la Galería")

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar Fuente de Imagen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera() // Si selecciona la cámara
                    1 -> openGallery() // Si selecciona la galería
                }
            }
            .show()
    }

    // Abrir la cámara
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        }
    }

    // Abrir la galería
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun guardarVehiculo() {
        val marca = binding.etMarca.text.toString().trim()
        val modelo = binding.etModelo.text.toString().trim()

        if (marca.isEmpty() || modelo.isEmpty() || imageUri == null) {
            showErrorDialog("Completa todos los campos")
            return
        }

        // Muestra el ProgressBar y deshabilita los botones
        progressBar.visibility = View.VISIBLE
        binding.btnGuardar.isEnabled = false
        binding.btnSeleccionarFoto.isEnabled = false

        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity?.currentFocus
        view?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }

        // Subir la imagen a Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference.child("car_images/${UUID.randomUUID()}.jpg")
        storageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val fotoUrl = uri.toString()
                    guardarDatosEnFirestore(marca, modelo, fotoUrl)  // Llama a la función que guarda en Firestore
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                binding.btnGuardar.isEnabled = true
                binding.btnSeleccionarFoto.isEnabled = true
                showErrorDialog("Error al subir imagen")
            }
    }

    private fun guardarDatosEnFirestore(marca: String, modelo: String, fotoUrl: String) {
        val db = FirebaseFirestore.getInstance()

        // Generar ID automático para el documento
        val vehiculoId = db.collection("vehiculos").document().id

        val vehiculo = hashMapOf(
            "ID" to vehiculoId,
            "Marca" to marca,
            "Modelo" to modelo,
            "Estatus" to "Activo",  // Por defecto, el vehículo será activo
            "Fecha_Alta" to System.currentTimeMillis().toString(), // Timestamp actual como String
            "FotoUrl" to fotoUrl
        )

        db.collection("vehiculos").document(vehiculoId)
            .set(vehiculo)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                binding.btnGuardar.isEnabled = true
                binding.btnSeleccionarFoto.isEnabled = true
                showSuccessDialog("Vehículo guardado correctamente")

                findNavController().navigate(R.id.action_addCarFragment_to_homeFragment)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                binding.btnGuardar.isEnabled = true
                binding.btnSeleccionarFoto.isEnabled = true
                showErrorDialog("Error al guardar: ${e.message}")
            }
    }

    // Función para mostrar un Dialog de error
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Función para mostrar un Dialog de éxito
    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Éxito")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            binding.ivFotoCarro.setImageURI(imageUri)
            binding.ivFotoCarro.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

