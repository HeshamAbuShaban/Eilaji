package dev.anonymous.eilaji.ui.pin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.anonymous.eilaji.databinding.FragmentPinConfirmationBinding

class FragmentPinConfirmation : Fragment() {
    private var binding: FragmentPinConfirmationBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPinConfirmationBinding.inflate(
            layoutInflater, container, false
        )
        binding!!.pinView.setAnimationEnable(true)
        binding!!.pinView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                println("onTextChanged $s")
            }

            override fun afterTextChanged(s: Editable) {}
        })
        return binding!!.root
    }

}