package dev.anonymous.eilaji.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.anonymous.eilaji.R;
import dev.anonymous.eilaji.models.MessageModel;
import dev.anonymous.eilaji.utils.GeneralUtils;

public class MessagesAdapter extends FirebaseRecyclerAdapter<MessageModel, RecyclerView.ViewHolder> {
    public static final int
            MSG_TYPE_SENDER = 0,
            MSG_TYPE_RECEIVER = 1,
            MSG_TYPE_SENDER_IMAGE = 2,
            MSG_TYPE_RECEIVER_IMAGE = 3;

    private final String userUid;
    private final FirebaseRecyclerOptions<MessageModel> options;
    private final boolean isRTL;

    public MessagesAdapter(@NonNull FirebaseRecyclerOptions<MessageModel> options, String userUid,
                           boolean isRTL) {
        super(options);
        this.options = options;
        this.userUid = userUid;
        this.isRTL = isRTL;
    }


    @Override
    public int getItemViewType(int position) {
        MessageModel message = options.getSnapshots().get(position);
        if (Objects.equals(message.getSenderUid(), userUid)) {
            if (message.getMessageImageUrl() != null)
                return MSG_TYPE_SENDER_IMAGE;
            else
                return MSG_TYPE_SENDER;
        } else {
            if (message.getMessageImageUrl() != null)
                return MSG_TYPE_RECEIVER_IMAGE;
            else
                return MSG_TYPE_RECEIVER;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == MSG_TYPE_SENDER) {
            return new TextMessageViewHolder(
                    inflater.inflate(
                            isRTL
                                    ? R.layout.item_outgoing_message_bubble_left
                                    : R.layout.item_outgoing_message_bubble_right
                            , parent, false)
            );
        } else if (viewType == MSG_TYPE_RECEIVER) {
            return new TextMessageViewHolder(
                    inflater.inflate(
                            isRTL
                                    ? R.layout.item_incoming_message_bubble_right
                                    : R.layout.item_incoming_message_bubble_left,
                            parent, false)
            );
        } else if (viewType == MSG_TYPE_SENDER_IMAGE) {
            return new ImageMessageViewHolder(
                    inflater.inflate(R.layout.item_outgoing_message_image, parent, false)
            );
        } else {
            // MSG_TYPE_RECEIVER_IMAGE
            return new ImageMessageViewHolder(
                    inflater.inflate(R.layout.item_incoming_message_image, parent, false)
            );
        }
    }

    @Override
    protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull MessageModel model) {
        if (model.getMessageImageUrl() != null) {
            ((ImageMessageViewHolder) holder).bind(model);
        } else {
            ((TextMessageViewHolder) holder).bind(model);
        }
    }

    public static class TextMessageViewHolder extends RecyclerView.ViewHolder {
        Context context;
        TextView tvMessageText, tvMessageDate;

        public TextMessageViewHolder(View view) {
            super(view);
            tvMessageText = view.findViewById(R.id.tvMessageText);
            tvMessageDate = view.findViewById(R.id.tvMessageDate);
            this.context = view.getContext();
        }

        void bind(MessageModel message) {
            tvMessageDate.setText(GeneralUtils.formatTimeStamp(message.getTimestamp()));

            if (message.getMedicineName() == null) {
                tvMessageText.setText(message.getMessageText());
            } else {
                checkMedicineId(
                        message.getMessageText(),
                        message.getMedicineName(),
                        tvMessageText
                );
            }
        }
        // ASDFJKJLK FSDJAFKJL SALDJFLJSAF
        // sdf sdf sd

        private void checkMedicineId(String massageText, String medicineName, TextView textView) {
            Pattern medicineIdPattern = Pattern.compile("id:([A-Za-z0-9_]+)");
            Matcher matcher = medicineIdPattern.matcher(massageText);

            if (matcher.find()) {
                String id = matcher.group();
                int startIndex = matcher.start();
                String newMessageText = massageText.replace(id, medicineName);
                int endIndex = startIndex + medicineName.length();

                SpannableString spanStr = new SpannableString(newMessageText);
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View textView) {
                        Toast.makeText(textView.getContext(), id, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void updateDrawState(TextPaint paint) {
                        super.updateDrawState(paint);

                        paint.setUnderlineText(false);
                        paint.setColor(ContextCompat.getColor(context, R.color.blue_light));
                        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                    }
                };
                spanStr.setSpan(
                        clickableSpan,
                        startIndex,
                        endIndex,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setHighlightColor(Color.TRANSPARENT);
                textView.setText(spanStr, TextView.BufferType.SPANNABLE);
            }
        }
    }


    public static class ImageMessageViewHolder extends RecyclerView.ViewHolder {
        Context context;
        TextView tvMessageImageDate;
        ImageView ivMessageImage;
        CircularProgressIndicator progressMessageImage;

        public ImageMessageViewHolder(View view) {
            super(view);
            tvMessageImageDate = view.findViewById(R.id.tvMessageImageDate);
            ivMessageImage = view.findViewById(R.id.ivMessageImage);
            progressMessageImage = view.findViewById(R.id.progressMessageImage);
            this.context = view.getContext();
        }

        void bind(MessageModel message) {
            GeneralUtils.getInstance().loadImage(Objects.requireNonNull(message.getMessageImageUrl()))
                    .listener(new RequestListener<>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            progressMessageImage.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            progressMessageImage.setVisibility(View.GONE);
                            return false;
                        }
                    }).into(ivMessageImage);

            tvMessageImageDate.setText(GeneralUtils.formatTimeStamp(message.getTimestamp()));
        }
    }
}