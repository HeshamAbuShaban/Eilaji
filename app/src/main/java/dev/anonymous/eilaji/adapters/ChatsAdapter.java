package dev.anonymous.eilaji.adapters;

import android.content.Context;
import android.content.Intent;
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
        holder.bind(model, userUid, getSnapshots().getSnapshot(position).getKey());
    }

    public static class ChatsViewHolder extends RecyclerView.ViewHolder {
        ItemChatBinding binding;
        Context context;

        public ChatsViewHolder(ItemChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
        }

        void bind(ChatModel chat, String userUid, String key) {
            binding.parentItemChat.setOnClickListener(v -> {
//                Intent intent = new Intent(context, MessagingActivity.class);
//                intent.putExtra("chat_id", chat.getChatId());
//                intent.putExtra("receiver_uid", key);
//                intent.putExtra("receiver_full_name", chat.getUserFullName());
//                intent.putExtra("receiver_image_url", chat.getUserImageUrl());
//                intent.putExtra("receiver_token", chat.getUserToken());
//                context.startActivity(intent);
            });

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
}