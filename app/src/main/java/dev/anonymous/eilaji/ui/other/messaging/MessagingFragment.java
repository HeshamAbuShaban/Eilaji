package dev.anonymous.eilaji.ui.other.messaging;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import dev.anonymous.eilaji.adapters.MessagesAdapter;
import dev.anonymous.eilaji.databinding.FragmentMessagingBinding;
import dev.anonymous.eilaji.models.ChatModel;
import dev.anonymous.eilaji.models.MessageModel;
import dev.anonymous.eilaji.network.ApiService;
import dev.anonymous.eilaji.network.ApiResponse;
import dev.anonymous.eilaji.network.ChatDto;
import dev.anonymous.eilaji.network.CreateChatRequest;
import dev.anonymous.eilaji.network.MessageDto;
import dev.anonymous.eilaji.network.NetworkModule;
import dev.anonymous.eilaji.network.PaginatedResult;
import dev.anonymous.eilaji.network.websocket.WebSocketManager;
import dev.anonymous.eilaji.storage.AppSharedPreferences;
import dev.anonymous.eilaji.utils.GeneralUtils;
import dev.anonymous.eilaji.utils.MyScrollToBottomObserver;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagingFragment extends Fragment {
    private static final String TAG = "MessagingFragment";
    private FragmentMessagingBinding binding;
    private ApiService apiService;
    private AppSharedPreferences prefs;
    private WebSocketManager webSocketManager;
    private MessagesAdapter messagesAdapter;
    private String chatId;
    private String userUid, userFullName, userUrlImage, userToken;
    private String receiverUid, receiverFullName, receiverUrlImage, receiverToken;
    String stringUri, description;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;
    private final List<MessageModel> messageList = new ArrayList<>();

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMessagingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        fetchUserData();
        setupClickListeners();
    }

    private void init() {
        prefs = AppSharedPreferences.getInstance(getActivity());
        apiService = NetworkModule.INSTANCE.provideApiService(requireContext());
        String token = prefs.getToken();
        if (token != null && !token.isEmpty()) webSocketManager = new WebSocketManager(token);
        initRegisterForActivityResult();
    }

    private void fetchUserData() {
        userUid = prefs.getUserId();
        userFullName = prefs.getFullName();
        userUrlImage = prefs.getImageUrl();
        userToken = prefs.getToken();
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
            if (receiverUrlImage != null) GeneralUtils.getInstance().loadImage(receiverUrlImage).circleCrop().into(binding.includeMessagingBarLayout.ivUserReceiverMessaging);
            binding.includeMessagingBarLayout.tvFullNameReceiverMessaging.setText(receiverFullName != null ? receiverFullName : "");
            if (chatId != null && !chatId.isEmpty()) {
                setupMessagesAdapter();
                loadMessages();
                connectWebSocket();
            } else {
                binding.progressMessaging.setVisibility(View.GONE);
            }
            sendPrescriptionIfExist(chatId != null && !chatId.isEmpty());
        } else binding.progressMessaging.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        binding.buSendMessage.setOnClickListener(v -> {
            String message = binding.edMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(message)) {
                binding.edMessage.setText("");
                if (chatId == null || chatId.isEmpty()) createNewChatAndSendMessage(message, null);
                else sendMessage(message);
            }
        });
        binding.buSendImage.setOnClickListener(v -> pickImageLauncher.launch(visualMediaRequest));
    }

    private void sendMessage(String message) {
        if (webSocketManager != null && chatId != null) {
            webSocketManager.sendMessage(chatId, message);
            MessageModel local = getMessageModel(message, null);
            local.setTimestamp(System.currentTimeMillis());
            messagesAdapter.addMessage(local);
            binding.recyclerMessaging.scrollToPosition(messagesAdapter.getItemCount() - 1);
            markAsRead();
        }
    }

    private final PickVisualMediaRequest visualMediaRequest = new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build();

    private void initRegisterForActivityResult() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                if (chatId == null || chatId.isEmpty()) createNewChatAndSendMessage(null, uri);
                else sendImage(uri);
            }
        });
    }

    private void sendImage(Uri uri) {
        Toast.makeText(getActivity(), "جار تحميل الصورة", Toast.LENGTH_SHORT).show();
        String url = uri.toString();
        if (webSocketManager != null && chatId != null) {
            webSocketManager.sendMessage(chatId, "image", "IMAGE", url);
            MessageModel local = getMessageModel(null, url);
            local.setTimestamp(System.currentTimeMillis());
            messagesAdapter.addMessage(local);
            binding.recyclerMessaging.scrollToPosition(messagesAdapter.getItemCount() - 1);
        }
    }

    private MessageModel getMessageModel(String message, String messageImageUrl) {
        return new MessageModel(userUid, receiverUid, message, messageImageUrl, null, System.currentTimeMillis());
    }

    private void createNewChatAndSendMessage(String message, Uri uri) {
        String pharmacyId = receiverUid;
        CreateChatRequest req = new CreateChatRequest(null, pharmacyId);
        binding.progressMessaging.setVisibility(View.VISIBLE);
        apiService.createChat(req).enqueue(new Callback<ApiResponse<ChatDto>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<ChatDto>> call, @NonNull Response<ApiResponse<ChatDto>> response) {
                binding.progressMessaging.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess() && response.body().getData() != null) {
                    chatId = response.body().getData().getId();
                    setupMessagesAdapter();
                    connectWebSocket();
                    if (message != null) sendMessage(message);
                    if (uri != null) sendImage(uri);
                } else Toast.makeText(getActivity(), "Failed to create chat", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<ChatDto>> call, @NonNull Throwable t) {
                binding.progressMessaging.setVisibility(View.GONE);
                Toast.makeText(getActivity(), "Failed to create chat: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendPrescriptionIfExist(boolean chatExists) {
        if (stringUri != null && !stringUri.isEmpty() && description != null && !description.isEmpty()) {
            if (chatExists) { sendImage(Uri.parse(stringUri)); sendMessage(description); }
            else createNewChatAndSendMessage(description, Uri.parse(stringUri));
        }
    }

    private void loadMessages() {
        if (chatId == null) return;
        binding.progressMessaging.setVisibility(View.VISIBLE);
        apiService.getMessages(chatId, 0, 50).enqueue(new Callback<ApiResponse<PaginatedResult<MessageDto>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<PaginatedResult<MessageDto>>> call, @NonNull Response<ApiResponse<PaginatedResult<MessageDto>>> response) {
                binding.progressMessaging.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess() && response.body().getData() != null) {
                    List<MessageDto> dtos = response.body().getData().getItems();
                    List<MessageModel> models = new ArrayList<>();
                    for (MessageDto dto : dtos) models.add(mapToUi(dto));
                    messagesAdapter.setMessages(models);
                    if (models.size() > 0) binding.recyclerMessaging.scrollToPosition(models.size() - 1);
                    markAsRead();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<PaginatedResult<MessageDto>>> call, @NonNull Throwable t) { binding.progressMessaging.setVisibility(View.GONE); }
        });
    }

    private MessageModel mapToUi(MessageDto dto) {
        String content = dto.getContent();
        String attachment = dto.getAttachmentUrl();
        String text = content;
        String image = null;
        if (attachment != null && !attachment.isEmpty()) image = attachment;
        else if (dto.getMessageType() != null && dto.getMessageType().equalsIgnoreCase("IMAGE") && content != null && content.startsWith("http")) { image = content; text = null; }
        long ts = parseTime(dto.getCreatedAt());
        return new MessageModel(dto.getSenderId(), null, text, image, null, ts);
    }

    private long parseTime(String s) {
        if (s == null) return System.currentTimeMillis();
        try { return Instant.parse(s).toEpochMilli(); } catch (Exception e) { try { return Long.parseLong(s); } catch (Exception ex) { return System.currentTimeMillis(); } }
    }

    private void setupMessagesAdapter() {
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setStackFromEnd(true);
        binding.recyclerMessaging.setLayoutManager(manager);
        boolean isRTL = binding.recyclerMessaging.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        messagesAdapter = new MessagesAdapter(userUid != null ? userUid : "", isRTL);
        binding.recyclerMessaging.setAdapter(messagesAdapter);
        messagesAdapter.registerAdapterDataObserver(new MyScrollToBottomObserver(binding.recyclerMessaging, messagesAdapter));
    }

    private void connectWebSocket() {
        if (webSocketManager == null || chatId == null) return;
        webSocketManager.setOnMessage(dto -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (dto.getChatId() != null && !dto.getChatId().equals(chatId)) return;
                messagesAdapter.addMessage(mapToUi(dto));
                binding.recyclerMessaging.scrollToPosition(messagesAdapter.getItemCount() - 1);
                markAsRead();
            });
        });
        webSocketManager.setOnRead(readChatId -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {});
        });
        webSocketManager.setOnPresence((uid, online) -> {});
        webSocketManager.setOnPong(() -> {});
        webSocketManager.connect();
        webSocketManager.sendJoin(chatId);
        webSocketManager.sendPing();
    }

    private void markAsRead() {
        if (chatId == null) return;
        apiService.markAsRead(chatId, new dev.anonymous.eilaji.network.MarkAsReadRequest(null, chatId)).enqueue(new Callback<ApiResponse<java.util.Map<String, Integer>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<java.util.Map<String, Integer>>> call, @NonNull Response<ApiResponse<java.util.Map<String, Integer>>> response) {}
            @Override public void onFailure(@NonNull Call<ApiResponse<java.util.Map<String, Integer>>> call, @NonNull Throwable t) {}
        });
        if (webSocketManager != null) webSocketManager.sendRead(chatId);
    }

    @Override public void onResume() {
        super.onResume();
        if (receiverUid != null) prefs.putCurrentUserChattingUID(receiverUid);
    }

    @Override public void onPause() {
        super.onPause();
        if (receiverUid != null) prefs.removeCurrentUserChattingUID();
        if (webSocketManager != null && chatId != null) webSocketManager.sendLeave(chatId);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (webSocketManager != null) webSocketManager.disconnect();
    }
}
