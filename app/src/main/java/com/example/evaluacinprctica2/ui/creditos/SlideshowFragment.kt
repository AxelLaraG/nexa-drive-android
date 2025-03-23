package com.example.evaluacinprctica2.ui.creditos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.evaluacinprctica2.R


class SlideshowFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout fragment_creditos.xml
        return inflater.inflate(R.layout.fragment_creditos, container, false)
    }
}