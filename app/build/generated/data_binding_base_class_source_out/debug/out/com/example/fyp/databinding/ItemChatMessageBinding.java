// Generated by view binder compiler. Do not edit!
package com.example.fyp.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.fyp.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ItemChatMessageBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final TextView messageTextView;

  private ItemChatMessageBinding(@NonNull LinearLayout rootView,
      @NonNull TextView messageTextView) {
    this.rootView = rootView;
    this.messageTextView = messageTextView;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ItemChatMessageBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ItemChatMessageBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.item_chat_message, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ItemChatMessageBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.messageTextView;
      TextView messageTextView = ViewBindings.findChildViewById(rootView, id);
      if (messageTextView == null) {
        break missingId;
      }

      return new ItemChatMessageBinding((LinearLayout) rootView, messageTextView);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
