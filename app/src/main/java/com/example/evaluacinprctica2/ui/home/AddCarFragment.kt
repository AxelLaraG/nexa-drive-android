package com.example.evaluacinprctica2.ui.home

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.evaluacinprctica2.R
import com.example.evaluacinprctica2.databinding.FragmentAddCarBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddCarFragment : Fragment() {

    private var _binding: FragmentAddCarBinding? = null
    private val binding get() = _binding!!
    private var imageUri: Uri? = null
    private val CAMERA_REQUEST_CODE = 2000
    private val CAMERA_PERMISSION_CODE = 100
    private val GALLERY_REQUEST_CODE = 1000
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

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }

        progressBar = binding.root.findViewById(R.id.progressBar)

        binding.btnSeleccionarFoto.setOnClickListener {
            showImageSourceDialog()
        }

        binding.btnGuardar.setOnClickListener {
            guardarVehiculo()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permiso de cámara concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    imageUri = data?.data
                }
                CAMERA_REQUEST_CODE -> {
                    if (imageUri == null) {
                        showErrorDialog("Error al capturar la imagen")
                        return
                    }
                }
            }
            val rotatedBitmap = fixImageRotation(imageUri!!)
            binding.ivFotoCarro.setImageBitmap(rotatedBitmap)
            binding.ivFotoCarro.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fixImageRotation(uri: Uri): Bitmap? {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val exif = ExifInterface(requireContext().contentResolver.openInputStream(uri)!!)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        val rotatedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }
        return rotatedBitmap
    }

    private fun rotateImage(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permiso de cámara no concedido", Toast.LENGTH_SHORT).show()
            return
        }

        val imageFile = File(requireContext().externalCacheDir, "${UUID.randomUUID()}.jpg")

        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } else {
            showErrorDialog("No se encontró una aplicación de cámara.")
        }
    }

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

        // Obtener la referencia del contador de IDs
        val counterRef = db.collection("config").document("vehiculo_counter")

        counterRef.get()
            .addOnSuccessListener { document ->
                try {
                    val currentCounter = document.getLong("counter") ?: 0L
                    val newID = currentCounter + 1

                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val currentDate = sdf.format(Date())

                    // Crear el objeto vehículo con el nuevo ID numérico
                    val vehiculo = hashMapOf(
                        "ID" to newID.toString(),
                        "Marca" to marca,
                        "Modelo" to modelo,
                        "Estatus" to "Activo",  // Por defecto, el vehículo será activo
                        "Fecha_Alta" to currentDate, // Timestamp actual como String
                        "FotoUrl" to fotoUrl
                    )

                    db.collection("vehiculos").document(newID.toString())
                        .set(vehiculo)
                        .addOnSuccessListener {
                            counterRef.update("counter", newID)
                                .addOnSuccessListener {
                                    progressBar.visibility = View.GONE
                                    binding.btnGuardar.isEnabled = true
                                    binding.btnSeleccionarFoto.isEnabled = true
                                    showSuccessDialog("Vehículo guardado correctamente")
                                    findNavController().navigate(R.id.action_addCarFragment_to_homeFragment)
                                }
                                .addOnFailureListener { e ->
                                    showErrorDialog("Error al actualizar contador: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            showErrorDialog("Error al guardar vehículo: ${e.message}")
                        }
                } catch (e: Exception) {
                    showErrorDialog("Error inesperado: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                binding.btnGuardar.isEnabled = true
                binding.btnSeleccionarFoto.isEnabled = true
                showErrorDialog("Error al obtener contador: ${e.message}")
                Log.e("FirestoreError", "Error al obtener contador: ${e.message}")
            }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Éxito")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

}

