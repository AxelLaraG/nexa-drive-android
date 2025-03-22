package com.example.evaluacinprctica2

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evaluacinprctica2.databinding.ActivityRegBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()


        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val passwordVal = binding.etConfirmPassword.text.toString()
            val nombre = binding.etNombre.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && passwordVal.isNotEmpty() && nombre.isNotEmpty() && (passwordVal == password)) {
                verificarUsuario(nombre, email, password)
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verificarUsuario(nombre: String, email: String, password: String) {
        db = FirebaseFirestore.getInstance()
        db.collection("usuarios")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { emailResult ->
                if (!emailResult.isEmpty) {
                    Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                db.collection("usuarios")
                    .whereEqualTo("nombre", nombre)
                    .get()
                    .addOnSuccessListener { nombreResult ->
                        if (!nombreResult.isEmpty) {
                            Toast.makeText(this, "El nombre ya está registrado", Toast.LENGTH_SHORT).show()
                        } else {
                            registrarUsuario(email, password, nombre)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al verificar nombre", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar email", Toast.LENGTH_SHORT).show()
            }
    }

    private fun registrarUsuario(email: String, password: String, nombre: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    val usuario = hashMapOf(
                        "id" to userId,
                        "nombre" to nombre,
                        "email" to email
                    )

                    db.collection("usuarios").document(userId!!)
                        .set(usuario)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al guardar usuario: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
