package dev.anonymous.eilaji.ui.base.user_interface.chatting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.firebase.ui.database.FirebaseRecyclerOptions
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.ChatsAdapter
import dev.anonymous.eilaji.adapters.ChatsAdapter.ChatListCallback
import dev.anonymous.eilaji.databinding.FragmentChattingBinding
import dev.anonymous.eilaji.firebase.FirebaseChatManager
import dev.anonymous.eilaji.models.ChatModel
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.ui.other.dialogs.AccountAccessDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.AccountAccessDialogFragment.AccountAccessListener
import dev.anonymous.eilaji.utils.LoadingDialog
class ChattingFragment : Fragment(), ChatListCallback, AccountAccessListener {
    private lateinit var binding: FragmentChattingBinding
    private lateinit var firebaseChatManager: FirebaseChatManager
    private lateinit var chattingViewModel: ChattingViewModel

    private var chatsAdapter: ChatsAdapter? = null
    private var userUid:String? = null
    private val loadingDialog = LoadingDialog()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChattingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseChatManager = FirebaseChatManager.getInstance()
        chattingViewModel = ViewModelProvider(this)[ChattingViewModel::class.java]
        observeUserData()
    }

    private fun observeUserData() {
        chattingViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                // set the userUID
                userUid = user.uid
                // User is logged in, now you are all set to display the chat
                setupChatsAdapter()
            } ?: run {
                // User is not logged in, show account access dialog
                AccountAccessDialogFragment.newInstance("Please create an account to use this feature!")
                    .show(childFragmentManager, "CantAccess!")
                // stop shimmer
                binding.shimmerChatContainer.stopShimmer()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Show Shimmer animation while data is loading
        startChatShimmer()
    }

    private fun setupChatsAdapter() {
        val options: FirebaseRecyclerOptions<ChatModel> = firebaseRecyclerOptions()

        if (options.snapshots.isNotEmpty()){
            removeChatShimmer()
        }
        chatsAdapter = ChatsAdapter(options, chattingViewModel.getUserUid())
        chatsAdapter?.setChatListCallback(this@ChattingFragment)


        // Assign the adapter and stop the Shimmer animation when data is loaded
        binding.recyclerChats.adapter = chatsAdapter



        chatsAdapter?.startListening()
    }

    // .. fetch the data
    private fun firebaseRecyclerOptions(): FirebaseRecyclerOptions<ChatModel> {
        val currentChatRef = firebaseChatManager.chatListRef.child(userUid!!)
        return FirebaseRecyclerOptions.Builder<ChatModel>()
            .setQuery(currentChatRef, ChatModel::class.java)
            .build()
    }

    private fun removeChatShimmer() {
        with(binding.shimmerChatContainer){
            stopShimmer()
            val isVisible = isVisible
            visibility = if (isVisible) View.GONE else View.VISIBLE
        }
    }
    private fun startChatShimmer() {
        with(binding.shimmerChatContainer){
            startShimmer()
            val isVisible = isVisible
            visibility = if (isVisible) View.VISIBLE else View.GONE
//            visibility = if (isVisible) View.GONE else View.VISIBLE
        }
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

    // AccountAccess ListenersCallback
    override fun onAllowClicked() {
        // restart the app, let the user create a new account
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
            // If there's no launch intent, navigate to the login screen
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.navigation_chatting, true)
                .build()
            findNavController().navigate(R.id.navigation_Login, null, navOptions)
        }
    }


    override fun onDenyClicked() {
        // do something to prevent the user from using the screen
        loadingDialog.show(childFragmentManager, "Load4ever")
    }
}
