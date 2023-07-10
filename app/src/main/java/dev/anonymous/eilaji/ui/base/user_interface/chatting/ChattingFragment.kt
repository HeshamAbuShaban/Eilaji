package dev.anonymous.eilaji.ui.base.user_interface.chatting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseUser
import dev.anonymous.eilaji.adapters.ChatsAdapter
import dev.anonymous.eilaji.databinding.FragmentChattingBinding
import dev.anonymous.eilaji.firebase.FirebaseChatManager
import dev.anonymous.eilaji.firebase.FirebaseController
import dev.anonymous.eilaji.models.ChatModel

class ChattingFragment : Fragment() {
    private var _binding: FragmentChattingBinding? = null
    private val binding get() = _binding!!

    private var chatsAdapter: ChatsAdapter? = null
    private var userUid: String? = null

    private var firebaseUser: FirebaseUser? = null
    private val firebaseController: FirebaseController = FirebaseController.getInstance()
    private var firebaseChatManager: FirebaseChatManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseUser = firebaseController.getCurrentUser()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChattingBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // now you are all set to display the chat
        executeChatDisplay()
    }

    private fun executeChatDisplay() {
        if (firebaseUser != null) {
            userUid = firebaseUser!!.uid
            firebaseChatManager = FirebaseChatManager()
            setupChatsAdapter()
        }
    }

    private fun setupChatsAdapter() {
        val currentChatRef = firebaseChatManager!!.chatListRef.child(userUid!!)
        val options: FirebaseRecyclerOptions<ChatModel> =
            FirebaseRecyclerOptions.Builder<ChatModel>()
                .setQuery(currentChatRef, ChatModel::class.java)
                .build()

        chatsAdapter = ChatsAdapter(options, userUid)
        binding.recyclerChats.layoutManager = LinearLayoutManager(activity)
        binding.recyclerChats.adapter = chatsAdapter
        chatsAdapter!!.startListening()
    }

    override fun onDestroyView() {
        chatsAdapter?.stopListening()
        _binding = null
        super.onDestroyView()
    }
}