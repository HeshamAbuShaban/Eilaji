package dev.anonymous.eilaji.ui.pin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.anonymous.eilaji.databinding.FragmentSentPinBinding

class FragmentSentPin : Fragment() {
    private var binding: FragmentSentPinBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSentPinBinding.inflate(layoutInflater, container, false)
        binding!!.buSend.setOnClickListener { v: View? -> }
        return binding!!.root
    }

}