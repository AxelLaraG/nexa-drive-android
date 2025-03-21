package com.example.evaluacinprctica2

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.evaluacinprctica2.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener {
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            navController.navigate(R.id.nav_add_car)
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_cuenta, R.id.nav_creditos, R.id.nav_logout
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Obtener la vista del header del NavigationView
        val headerView = navView.getHeaderView(0)
        val tvUserEmail: TextView = headerView.findViewById(R.id.tvUserEmail) // Referencia al TextView

        // Obtener usuario de Firebase
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            tvUserEmail.text = user.email // Mostrar el email del usuario autenticado
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    Snackbar.make(binding.root, "Inicio seleccionado", Snackbar.LENGTH_SHORT).show()
                }
                R.id.nav_cuenta -> {
                    Snackbar.make(binding.root, "Cuenta seleccionada", Snackbar.LENGTH_SHORT).show()
                }
                R.id.nav_creditos -> {
                    Snackbar.make(binding.root, "Créditos seleccionados", Snackbar.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut() // Cerrar sesión en Firebase
                    startActivity(Intent(this, LoginActivity::class.java)) // Redirigir a Login
                    finish()
                }
            }
            binding.drawerLayout.closeDrawers() // Cerrar el menú lateral después de hacer clic
            true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
