package dev.anonymous.eilaji.ui.other.messaging;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

import dev.anonymous.eilaji.adapters.MessagesAdapter;
import dev.anonymous.eilaji.databinding.FragmentMessagingBinding;
import dev.anonymous.eilaji.firebase.FirebaseChatManager;
import dev.anonymous.eilaji.firebase.notification.APIService;
import dev.anonymous.eilaji.firebase.notification.Client;
import dev.anonymous.eilaji.firebase.notification.Data;
import dev.anonymous.eilaji.firebase.notification.MyResponse;
import dev.anonymous.eilaji.firebase.notification.Sender;
import dev.anonymous.eilaji.models.ChatModel;
import dev.anonymous.eilaji.models.MessageModel;
import dev.anonymous.eilaji.storage.AppSharedPreferences;
import dev.anonymous.eilaji.utils.GeneralUtils;
import dev.anonymous.eilaji.utils.MyScrollToBottomObserver;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagingFragment extends Fragment {
    private static final String TAG = "MessagingFragment";

    private FragmentMessagingBinding binding;

    private APIService apiService;

    private FirebaseChatManager firebaseChatManager;
    private AppSharedPreferences chatSharedPreferencesManager;

    private MessagesAdapter messagesAdapter;

    private String chatId;

    private String userUid,
            userFullName,
            userUrlImage,
            userToken;

    private String receiverUid,
            receiverFullName,
            receiverUrlImage,
            receiverToken;

    String stringUri, description;

    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMessagingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        init();
        fetchUserData();
        setupClickListeners();
    }

    private void init() {
        firebaseChatManager = new FirebaseChatManager();
        chatSharedPreferencesManager = AppSharedPreferences.getInstance(getActivity());

        apiService = Client.getClient().create(APIService.class);

        initRegisterForActivityResult();
    }

    private void fetchUserData() {
        FirebaseUser user = firebaseChatManager.getCurrentUser();
        if (user != null) {
            userUid = user.getUid();
            userFullName = chatSharedPreferencesManager.getFullName();
            userUrlImage = chatSharedPreferencesManager.getImageUrl();
            userToken = chatSharedPreferencesManager.getToken();

            Bundle arguments = getArguments();
            if (arguments != null) {
                MessagingFragmentArgs args = MessagingFragmentArgs.fromBundle(arguments);

                chatId = args.getChatId();
                receiverUid = args.getReceiverUid();
                receiverFullName = args.getReceiverFullName();
                receiverUrlImage = args.getReceiverUrlImage();
                receiverToken = args.getReceiverToken();

                stringUri = args.getStringUri();
                description = args.getDescription();

                GeneralUtils.getInstance()
                        .loadImage(receiverUrlImage)
                        .circleCrop()
                        .into(binding.includeMessagingBarLayout.ivUserReceiverMessaging);

                binding.includeMessagingBarLayout.tvFullNameReceiverMessaging.setText(receiverFullName);

                loadChatIfExist();
            }
        }
    }

    private void setupClickListeners() {
        binding.buSendMessage.setOnClickListener(v -> {
            String message = binding.edMessage.getText().toString().trim();
            if (chatId != null && !TextUtils.isEmpty(message)) {
                binding.edMessage.setText("");
                if (chatId.isEmpty()) {
                    createNewChatAndSendMessage(message, null);
                } else {
                    sendMessage(message);
                }
            }
        });
        binding.buSendImage.setOnClickListener(v -> pickImageLauncher.launch(visualMediaRequest));
    }

    private void sendMessage(String message) {
        firebaseChatManager.addMessageToChat(
                chatId, getMessageModel(message, null)
        );

        updateChatListUsers(message, null);

        sendNotification(message, null);
    }

    private final PickVisualMediaRequest visualMediaRequest =
            new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build();

    private void initRegisterForActivityResult() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (chatId != null && uri != null) {
                if (chatId.isEmpty()) {
                    createNewChatAndSendMessage(null, uri);
                } else {
                    sendImage(uri);
                }
            }
        });
    }

    private void sendImage(Uri uri) {
        Toast.makeText(getActivity(), "جار تحميل الصورة", Toast.LENGTH_SHORT).show();
        firebaseChatManager.uploadImageMessage(
                userUid,
                "MessageImage",
                uri, (imageUrl, success) -> {
                    if (success) {
                        firebaseChatManager.addMessageToChat(
                                chatId, getMessageModel(null, imageUrl)
                        );

                        updateChatListUsers(null, imageUrl);

                        sendNotification(null, imageUrl);
                    }
                }
        );
    }

    void updateChatListUsers(String message, String messageImageUrl) {
        firebaseChatManager.updateChatList(
                userUid,
                receiverUid,
                getChatModelSender(message, messageImageUrl)
        );

        firebaseChatManager.updateChatList(
                receiverUid,
                userUid,
                getChatModelReceiver(message, messageImageUrl)
        );
    }


    private MessageModel getMessageModel(String message, String messageImageUrl) {
        return new MessageModel(
                userUid,
                receiverUid,
                message,
                messageImageUrl,
                null,
                System.currentTimeMillis()
        );
    }

    private ChatModel getChatModelSender(String message, String lastMessageImageUrl) {
        return new ChatModel(
                chatId,
                message,
                lastMessageImageUrl,
                userUid,
                receiverFullName,
                receiverUrlImage,
                receiverToken,
                System.currentTimeMillis()
        );
    }

    private ChatModel getChatModelReceiver(String message, String lastMessageImageUrl) {
        return new ChatModel(
                chatId,
                message,
                lastMessageImageUrl,
                userUid,
                userFullName,
                userUrlImage,
                userToken,
                System.currentTimeMillis()
        );
    }

    private void createNewChatAndSendMessage(String message, Uri uri) {
        firebaseChatManager.createChat(userUid, receiverUid, (id, success) -> {
            if (success) {
                chatId = id;

                if (message != null) {
                    sendMessage(message);
                }
                if (uri != null) {
                    sendImage(uri);
                }

                setupMessagesAdapter();
            } else {
                Toast.makeText(getActivity(), "Failed to create chat", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendNotification(String message, String messageImageUrl) {
        Data data = new Data(userUid, userFullName, message, userUrlImage, messageImageUrl);
        Sender sender = new Sender(data, receiverToken);
        sendNotificationFCM(sender);
    }

    private void sendNotificationFCM(Sender sender) {
        apiService.sendNotification(sender)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<MyResponse> call, @NonNull Response<MyResponse> response) {
                        if (response.code() == 200) {
                            if (response.body() != null) {
                                if (response.body().getSuccess() == 1) {
                                    Toast.makeText(getActivity(), "تم ارسال الاشعار بنجاح", Toast.LENGTH_SHORT).show();
                                } else {
                                    String error = response.body().getResults().get(0).getError();
                                    if (error.equals("NotRegistered")) {
                                        Toast.makeText(getActivity(), "هذا المسخدم غير موجود", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getActivity(), "لم يتم ارسال الاشعار", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "onResponse: " + error);
                                    }
                                }
                            }
                        }
                    }

                    // // {"multicast_id":6143843070518083714,"success":0,"failure":1,"canonical_ids":0,"results":[{"error":"NotRegistered"}]}
                    @Override
                    public void onFailure(@NonNull Call<MyResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "onFailure: " + t.getMessage());
                    }
                });
    }

    private void loadChatIfExist() {
        firebaseChatManager.checkChatExist(userUid, receiverUid, (exists, chatIdValue) -> {
            binding.progressMessaging.setVisibility(View.GONE);
            chatId = chatIdValue;
            if (exists) {
                setupMessagesAdapter();
            }
            sendPrescriptionIfExist(exists);
        });
    }

    private void sendPrescriptionIfExist(boolean chatExists) {
        if (stringUri != null && !stringUri.isEmpty()
                && description != null && !description.isEmpty()) {
            if (chatExists) {
                sendImage(Uri.parse(stringUri));
                sendMessage(description);
            } else {
                createNewChatAndSendMessage(description, Uri.parse(stringUri));
            }
        }
    }

    private void setupMessagesAdapter() {
        DatabaseReference currentChatRef = firebaseChatManager.getMessagingDataReference(chatId);
        FirebaseRecyclerOptions<MessageModel> options
                = firebaseChatManager.getMessageModelOptions(currentChatRef);

        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        // Scroll to end of recycler
        manager.setStackFromEnd(true);
        binding.recyclerMessaging.setLayoutManager(manager);

        boolean isRTL = binding.recyclerMessaging.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        messagesAdapter = new MessagesAdapter(options, userUid, isRTL);
        binding.recyclerMessaging.setAdapter(messagesAdapter);

        messagesAdapter.startListening();

        messagesAdapter.registerAdapterDataObserver(
                new MyScrollToBottomObserver(
                        binding.recyclerMessaging,
                        messagesAdapter
                )
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        if (receiverUid != null) {
            chatSharedPreferencesManager.putCurrentUserChattingUID(receiverUid);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (receiverUid != null) {
            chatSharedPreferencesManager.removeCurrentUserChattingUID();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (messagesAdapter != null) {
            messagesAdapter.stopListening();
        }
    }
}