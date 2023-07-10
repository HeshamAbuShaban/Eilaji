package dev.anonymous.eilaji.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import dev.anonymous.eilaji.R;
import dev.anonymous.eilaji.databinding.ItemChatBinding;
import dev.anonymous.eilaji.models.ChatModel;
import dev.anonymous.eilaji.utils.GeneralUtils;

public class ChatsAdapter extends FirebaseRecyclerAdapter<ChatModel, ChatsAdapter.ChatsViewHolder> {
    private final String userUid;
    private ChatListCallback chatListCallback;

    public void setChatListCallback(ChatListCallback chatListCallback) {
        this.chatListCallback = chatListCallback;
    }

    public ChatsAdapter(@NonNull FirebaseRecyclerOptions<ChatModel> options, String userUid) {
        super(options);
        this.userUid = userUid;
    }

    @NonNull
    @Override
    public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChatsViewHolder(ItemChatBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        ));
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatsViewHolder holder, int position, @NonNull ChatModel model) {
        holder.bind(
                position,
                model,
                userUid,
                getSnapshots().getSnapshot(position).getKey()
        );
        holder.setChatListCallback(chatListCallback);
    }

    public static class ChatsViewHolder extends RecyclerView.ViewHolder {
        ItemChatBinding binding;
        Context context;
        private ChatListCallback chatListCallback;

        protected void setChatListCallback(ChatListCallback chatListCallback) {
            this.chatListCallback = chatListCallback;
        }

        public ChatsViewHolder(ItemChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
        }

        void bind(int itemPosition, ChatModel chat, String userUid, String key) {

            if (itemPosition == 0) {
                // margin top first tow item
                binding.parentChatItem.setPadding(0, 40, 0, 0);
            }

            binding.parentItemChat.setOnClickListener(v ->
                    chatListCallback.onChatItemClicked(chat, key));

            if (chat.getUserImageUrl() != null && chat.getUserImageUrl().equals("default")) {
                binding.ivUserReceiver.setImageResource(R.drawable.ic_default_user);
            } else if (chat.getUserImageUrl() != null) {
                GeneralUtils.getInstance().loadImage(chat.getUserImageUrl()).into(binding.ivUserReceiver);
            }

            binding.tvUserReceiver.setText(chat.getUserFullName());
            binding.tvLastMassageDate.setText(GeneralUtils.formatTimeStamp(chat.getTimestamp()));

            if (chat.getLastMessageImageUrl() != null && !chat.getLastMessageImageUrl().isEmpty()) {
                binding.tvLastMessageText.setText(
                        userUid.equals(chat.getLastMessageSenderUid())
                                ? "you: image"
                                : "image"
                );
            } else {
                binding.tvLastMessageText.setText(
                        userUid.equals(chat.getLastMessageSenderUid())
                                ? "you: " + chat.getLastMessageText()
                                : chat.getLastMessageText()
                );
            }
        }
    }

    public interface ChatListCallback {
        void onChatItemClicked(ChatModel chatModel , String key);
    }
}