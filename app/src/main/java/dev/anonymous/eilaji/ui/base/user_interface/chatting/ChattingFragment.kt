package dev.anonymous.eilaji.ui.base.user_interface.chatting

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseUser
import dev.anonymous.eilaji.adapters.ChatsAdapter
import dev.anonymous.eilaji.adapters.ChatsAdapter.ChatListCallback
import dev.anonymous.eilaji.databinding.FragmentChattingBinding
import dev.anonymous.eilaji.firebase.FirebaseChatManager
import dev.anonymous.eilaji.firebase.FirebaseController
import dev.anonymous.eilaji.models.ChatModel
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.main.MainActivity
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.ui.other.dialogs.AccountAccessDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.AccountAccessDialogFragment.AccountAccessListener
import dev.anonymous.eilaji.utils.LoadingDialog

class ChattingFragment : Fragment(), ChatListCallback, AccountAccessListener {
    private lateinit var _binding: FragmentChattingBinding
    private val binding get() = _binding

    private var chatsAdapter: ChatsAdapter? = null

    private lateinit var firebaseUser: FirebaseUser
    private lateinit var userUid: String

    private lateinit var firebaseChatManager: FirebaseChatManager

    private val loadingDialog = LoadingDialog()


    // Listener

    /*fun setOnNavigateToMessagingListener(listener: OnNavigateToMessagingListener) {
        navigateToMessagingListener = listener
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initComponent()
    }

    private fun initComponent() {
        _binding = FragmentChattingBinding.inflate(layoutInflater)

        firebaseChatManager = FirebaseChatManager.getInstance()
        val firebaseController: FirebaseController = FirebaseController.getInstance()
        val currentUser = firebaseController.getCurrentUser()
        if (currentUser != null) {
            firebaseUser = currentUser
            userUid = firebaseUser.uid
            // now you are all set to display the chat
            executeChatDisplay()
        } else {
            AccountAccessDialogFragment.newInstance("Please Create an account to use this feature!")
                .show(childFragmentManager, "CantAccess!")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("ChattingFragment", "onViewCreated: TODO:More Work need to be done in this class")
//        loadingDialog.show(childFragmentManager, "Loading")
    }

    private fun executeChatDisplay() {
        setupChatsAdapter()
    }

    private fun setupChatsAdapter() {
        val currentChatRef = firebaseChatManager.chatListRef.child(userUid)
        val options: FirebaseRecyclerOptions<ChatModel> =
            FirebaseRecyclerOptions.Builder<ChatModel>()
                .setQuery(currentChatRef, ChatModel::class.java)
                .build()

        // TODO :dismiss the dialog IN A PROPER WAY...... it doesn't work like this
//        loadingDialog.dismiss()
        chatsAdapter = ChatsAdapter(options, userUid)
        chatsAdapter?.setChatListCallback(this@ChattingFragment)
        binding.recyclerChats.layoutManager = LinearLayoutManager(activity)
        binding.recyclerChats.adapter = chatsAdapter
        chatsAdapter?.startListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatsAdapter?.stopListening()
    }

    override fun onChatItemClicked(chatModel: ChatModel, key: String) {
        val intent = Intent(requireContext(), AlternativesActivity::class.java)
        intent.putExtra("fragmentType", FragmentsKeys.messaging.name)
        intent.putExtra("chatId", chatModel.chatId)
        intent.putExtra("receiverUid", key)
        intent.putExtra("receiverFullName", chatModel.userFullName)
        intent.putExtra("receiverUrlImage", chatModel.userImageUrl)
        intent.putExtra("receiverToken", chatModel.userToken)
        startActivity(intent)
    }

    override fun onAllowClicked() {
        // restart the app , to let the user create a new account
        createAccountLogic()
    }

    // this method takes the user to the login screen to make him create an account
    private fun createAccountLogic() {
        val packageManager = requireContext().packageManager
        val packageName = requireContext().packageName
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            requireActivity().finish()
        } else {
            // pop the current screen from the back stack
            requireActivity().finish()
            // move to the login screen
            val intent2 = Intent(requireContext(), MainActivity::class.java)
            intent2.putExtra(
                "logoutTrigger",
                FragmentsKeys.logout.name
            )
            startActivity(intent2)
        }
    }

    override fun onDenyClicked() {
        loadingDialog.show(childFragmentManager, "Load4ever")
    }

}