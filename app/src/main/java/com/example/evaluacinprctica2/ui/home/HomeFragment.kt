package com.example.evaluacinprctica2.ui.home

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.evaluacinprctica2.R
import com.example.evaluacinprctica2.adapters.CarAdapter
import com.example.evaluacinprctica2.models.Car
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var carAdapter: CarAdapter
    private lateinit var carList: MutableList<Car>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchView: SearchView
    private lateinit var videoView: VideoView
    private lateinit var btnFilter: Button

    override fun onResume() {
        super.onResume()
        loadCars()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout)
        recyclerView = rootView.findViewById(R.id.recyclerViewCars)
        searchView = rootView.findViewById(R.id.searchView)
        videoView = rootView.findViewById(R.id.videoView)
        btnFilter = rootView.findViewById(R.id.btnFilter)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        carList = mutableListOf()

        // Modificar el adaptador para que maneje clics
        carAdapter = CarAdapter(carList) { car ->
            openEditCarFragment(car)
        }

        recyclerView.adapter = carAdapter

        loadCars()

        swipeRefreshLayout.setOnRefreshListener {
            loadCars()
        }

        //setupVideoPlayer()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })

        btnFilter.setOnClickListener {
            showFilterDialog()
        }

        return rootView
    }

    private fun loadCars() {
        val db = FirebaseFirestore.getInstance()
        db.collection("vehiculos")
            .get()
            .addOnSuccessListener { documents ->
                carList.clear()
                for (document in documents) {
                    val car = document.toObject(Car::class.java)
                    carList.add(car)
                }

                carAdapter.updateList(carList)
                recyclerView.adapter = carAdapter
                carAdapter.notifyDataSetChanged()

                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
                swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun openEditCarFragment(car: Car) {
    // Crear un Bundle con los datos que quieras pasar al siguiente fragmento
    val bundle = Bundle().apply {
        putParcelable("car", car) // Pasa el objeto Car
    }

    // Navegar usando el NavController y pasar el Bundle con la acción
    findNavController().navigate(R.id.action_home_to_editCar, bundle)
}


    private fun filterList(query: String?) {
        val filteredList = carList.filter { car ->
            car.ID.contains(query ?: "", true) ||
                    car.Modelo.contains(query ?: "", true) ||
                    car.Marca.contains(query ?: "", true)
        }
        carAdapter.updateList(filteredList)
    }

    private fun showFilterDialog() {
        val options = arrayOf("Mostrar solo activos", "Mostrar solo inactivos", "Registrados después de cierta fecha", "Mostrar todos")
        AlertDialog.Builder(requireContext())
            .setTitle("Selecciona un filtro")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> carAdapter.updateList(carList.filter { it.Estatus == "Activo" })
                    1 -> carAdapter.updateList(carList.filter { it.Estatus == "Inactivo" })
                    2 -> showDatePicker()
                    3 -> loadCars()
                }
            }
            .show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDateString = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val selectedDate = LocalDate.parse(selectedDateString, formatter)

            carAdapter.updateList(carList.filter {
                val vehiculoDate = LocalDate.parse(it.Fecha_Alta, formatter)
                vehiculoDate.isAfter(selectedDate) || vehiculoDate.isEqual(selectedDate)
            })
        }, year, month, day)

        datePicker.show()
    }
}
