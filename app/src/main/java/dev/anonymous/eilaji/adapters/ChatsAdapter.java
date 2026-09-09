package dev.anonymous.eilaji.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dev.anonymous.eilaji.R;
import dev.anonymous.eilaji.databinding.ItemChatBinding;
import dev.anonymous.eilaji.models.ChatModel;
import dev.anonymous.eilaji.utils.GeneralUtils;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatsViewHolder> {
    private final String userUid;
    private final List<ChatModel> chats = new ArrayList<>();
    private ChatListCallback chatListCallback;

    public void setChatListCallback(ChatListCallback chatListCallback) { this.chatListCallback = chatListCallback; }

    public ChatsAdapter(String userUid) { this.userUid = userUid; }

    public ChatsAdapter(@NonNull List<ChatModel> initial, String userUid) {
        this.userUid = userUid;
        if (initial != null) chats.addAll(initial);
    }

    public void setChats(List<ChatModel> list) {
        chats.clear();
        if (list != null) chats.addAll(list);
        notifyDataSetChanged();
    }

    @Override public int getItemCount() { return chats.size(); }

    @NonNull @Override public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChatsViewHolder(ItemChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ChatsViewHolder holder, int position) {
        ChatModel model = chats.get(position);
        holder.bind(position, model, userUid, model.getChatId() != null ? model.getChatId() : "");
        holder.setChatListCallback(chatListCallback);
    }

    public static class ChatsViewHolder extends RecyclerView.ViewHolder {
        ItemChatBinding binding; Context context; private ChatListCallback chatListCallback;
        protected void setChatListCallback(ChatListCallback chatListCallback) { this.chatListCallback = chatListCallback; }
        public ChatsViewHolder(ItemChatBinding binding) { super(binding.getRoot()); this.binding = binding; this.context = binding.getRoot().getContext(); }
        void bind(int itemPosition, ChatModel chat, String userUid, String key) {
            if (itemPosition == 0) binding.parentChatItem.setPadding(0, 40, 0, 0);
            binding.parentItemChat.setOnClickListener(v -> { if (chatListCallback != null) chatListCallback.onChatItemClicked(chat, key); });
            if (chat.getUserImageUrl() != null && chat.getUserImageUrl().equals("default")) binding.ivUserReceiver.setImageResource(R.drawable.ic_default_user);
            else if (chat.getUserImageUrl() != null) GeneralUtils.getInstance().loadImage(chat.getUserImageUrl()).into(binding.ivUserReceiver);
            binding.tvUserReceiver.setText(chat.getUserFullName() != null ? chat.getUserFullName() : "Chat");
            binding.tvLastMassageDate.setText(chat.getTimestamp() != null ? GeneralUtils.formatTimeStamp(chat.getTimestamp()) : "");
            if (chat.getLastMessageImageUrl() != null && !chat.getLastMessageImageUrl().isEmpty()) binding.tvLastMessageText.setText(userUid != null && userUid.equals(chat.getLastMessageSenderUid()) ? "you: image" : "image");
            else binding.tvLastMessageText.setText(chat.getLastMessageText() != null ? (userUid != null && userUid.equals(chat.getLastMessageSenderUid()) ? "you: " + chat.getLastMessageText() : chat.getLastMessageText()) : "");
        }
    }

    public interface ChatListCallback { void onChatItemClicked(ChatModel chatModel, String key); }
}
