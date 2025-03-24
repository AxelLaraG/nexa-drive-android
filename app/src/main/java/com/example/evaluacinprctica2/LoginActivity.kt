package com.example.evaluacinprctica2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evaluacinprctica2.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        checkUserSession()

        binding.btnLogin.setOnClickListener {
            val nombre = binding.etEmail.text.toString().trim() // Ahora es el nombre, no el email
            val password = binding.etPassword.text.toString().trim()

            if (nombre.isNotEmpty() && password.isNotEmpty()) {
                obtenerCorreoYAutenticar(nombre, password)
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            val nombre = binding.etEmail.text.toString().trim()
            if (nombre.isNotEmpty()) {
                obtenerCorreoParaRecuperacion(nombre)
            } else {
                Toast.makeText(this, "Por favor, ingresa tu nombre de usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerCorreoYAutenticar(nombre: String, password: String) {
        db.collection("usuarios")
            .whereEqualTo("nombre", nombre)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val email = documents.documents[0].getString("email")
                    if (email != null) {
                        signIn(email, password)
                    } else {
                        Toast.makeText(this, "Error: No se encontró el email asociado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al buscar usuario", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signIn(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.tvRegister.isEnabled = false
        binding.tvForgotPassword.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish() // Cierra LoginActivity para que no vuelva atrás
                } else {
                    binding.btnLogin.isEnabled = true
                    binding.tvRegister.isEnabled = true
                    binding.tvForgotPassword.isEnabled = true

                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun obtenerCorreoParaRecuperacion(nombre: String) {
        db.collection("usuarios")
            .whereEqualTo("nombre", nombre)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val email = documents.documents[0].getString("email")
                    if (email != null) {
                        resetPassword(email)
                    } else {
                        Toast.makeText(this, "Error: No se encontró el email asociado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al buscar usuario", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Correo de recuperación enviado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserSession() {
        val user = auth.currentUser
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

