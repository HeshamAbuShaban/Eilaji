package dev.anonymous.eilaji.ui.user_interface.send_prescription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.anonymous.eilaji.databinding.FragmentSendPrescriptionBinding

class SendPrescriptionFragment : Fragment() {

    private lateinit var _binding: FragmentSendPrescriptionBinding
    private lateinit var sendPrescriptionViewModel: SendPrescriptionViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSendPrescriptionBinding.inflate(inflater, container, false)
        sendPrescriptionViewModel = ViewModelProvider(this)[SendPrescriptionViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}