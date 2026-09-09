package dev.anonymous.eilaji.ui.base.user_interface.chatting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.ChatsAdapter
import dev.anonymous.eilaji.adapters.ChatsAdapter.ChatListCallback
import dev.anonymous.eilaji.databinding.FragmentChattingBinding
import dev.anonymous.eilaji.models.ChatModel
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.ui.other.dialogs.AccountAccessDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.AccountAccessDialogFragment.AccountAccessListener
import dev.anonymous.eilaji.utils.LoadingDialog

class ChattingFragment : Fragment(), ChatListCallback, AccountAccessListener {
    private lateinit var binding: FragmentChattingBinding
    private lateinit var chattingViewModel: ChattingViewModel
    private var chatsAdapter: ChatsAdapter? = null
    private var userUid: String? = null
    private val loadingDialog = LoadingDialog()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentChattingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chattingViewModel = ViewModelProvider(this)[ChattingViewModel::class.java]
        chattingViewModel.init(requireContext())
        observeUserData()
        observeChats()
        if (chattingViewModel.isLoggedIn()) chattingViewModel.loadChats(requireContext()) else binding.shimmerChatContainer.stopShimmer()
    }

    private fun observeUserData() {
        if (!chattingViewModel.isLoggedIn()) {
            AccountAccessDialogFragment.newInstance("Please create an account to use this feature!").show(childFragmentManager, "CantAccess!")
            binding.shimmerChatContainer.stopShimmer()
        } else {
            userUid = chattingViewModel.getUserUid()
            setupChatsAdapter()
        }
    }

    private fun observeChats() {
        chattingViewModel.chats.observe(viewLifecycleOwner) { list ->
            chatsAdapter?.setChats(list)
            removeChatShimmer()
            if (list.isEmpty()) {
                Toast.makeText(requireContext(), "No chats yet", Toast.LENGTH_SHORT).show()
            }
        }
        chattingViewModel.isEmpty.observe(viewLifecycleOwner) { empty ->
            if (empty == true) removeChatShimmer()
        }
        chattingViewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() { super.onStart(); startChatShimmer() }

    private fun setupChatsAdapter() {
        chatsAdapter = ChatsAdapter(chattingViewModel.getUserUid())
        chatsAdapter?.setChatListCallback(this@ChattingFragment)
        binding.recyclerChats.adapter = chatsAdapter
    }

    private fun removeChatShimmer() {
        with(binding.shimmerChatContainer) { stopShimmer(); visibility = if (isVisible) View.GONE else View.VISIBLE }
    }
    private fun startChatShimmer() {
        with(binding.shimmerChatContainer) { startShimmer(); visibility = if (isVisible) View.VISIBLE else View.GONE }
    }

    override fun onDestroyView() { super.onDestroyView(); chatsAdapter = null }

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

    override fun onAllowClicked() { createAccountLogic() }
    private fun createAccountLogic() {
        val packageManager = requireContext().packageManager
        val packageName = requireContext().packageName
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); startActivity(intent); requireActivity().finish() }
        else { val navOptions = NavOptions.Builder().setPopUpTo(R.id.navigation_chatting, true).build(); findNavController().navigate(R.id.navigation_Login, null, navOptions) }
    }
    override fun onDenyClicked() { loadingDialog.show(childFragmentManager, "Load4ever") }
}
