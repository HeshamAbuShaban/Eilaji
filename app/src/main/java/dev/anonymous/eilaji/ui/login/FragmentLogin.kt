package dev.anonymous.eilaji.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dev.anonymous.eilaji.ui.singup.FragmentSingUp
import dev.anonymous.eilaji.ui.home.HomeActivity
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentLoginBinding

class FragmentLogin : Fragment() {
    var binding: FragmentLoginBinding? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        auth = Firebase.auth
        binding!!.buForgotYourPassword.setOnClickListener { v: View? -> }
        binding!!.buSignUp.setOnClickListener {
            val singUpFragment = FragmentSingUp()
            val fm = parentFragmentManager
            val tr = fm.beginTransaction()
            tr.replace(R.id.mainActivityContainer, singUpFragment)
            tr.commitAllowingStateLoss()
        }
        binding!!.buLogin.setOnClickListener {
            val email = binding!!.edEmail.text.toString()
            val password = binding!!.edPassword.text.toString()
            if (email.isEmpty()) {
                binding!!.edEmail.error = "Please Enter Your Email"
            }
            if (password.isEmpty()) {
                binding!!.edPassword.error = "Please Enter Your Password"
            }
            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("fix", "createUserWithEmail:success")

                            startActivity(Intent(requireContext(), HomeActivity::class.java))
                            activity?.finish()


                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("fix", "createUserWithEmail:failure", task.exception)
                            Toast.makeText(
                                requireActivity(),
                                "Authentication failed.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
            }
        }
        return binding!!.root
    }

}