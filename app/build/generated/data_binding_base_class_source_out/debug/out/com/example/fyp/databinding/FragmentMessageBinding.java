// Generated by view binder compiler. Do not edit!
package com.example.fyp.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.fyp.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentMessageBinding implements ViewBinding {
  @NonNull
  private final RelativeLayout rootView;

  @NonNull
  public final Button btnChat;

  @NonNull
  public final Button btnExplore;

  @NonNull
  public final Button btnNotification;

  @NonNull
  public final Button btnOrder;

  @NonNull
  public final LinearLayout buttonRow;

  @NonNull
  public final CardView cardAction;

  @NonNull
  public final CardView cardChat;

  @NonNull
  public final CardView cardImage;

  @NonNull
  public final CardView cardNotification;

  @NonNull
  public final CardView cardOrder;

  @NonNull
  public final CardView descriptionCard;

  @NonNull
  public final TextView descriptionText;

  @NonNull
  public final ImageView imageView;

  private FragmentMessageBinding(@NonNull RelativeLayout rootView, @NonNull Button btnChat,
      @NonNull Button btnExplore, @NonNull Button btnNotification, @NonNull Button btnOrder,
      @NonNull LinearLayout buttonRow, @NonNull CardView cardAction, @NonNull CardView cardChat,
      @NonNull CardView cardImage, @NonNull CardView cardNotification, @NonNull CardView cardOrder,
      @NonNull CardView descriptionCard, @NonNull TextView descriptionText,
      @NonNull ImageView imageView) {
    this.rootView = rootView;
    this.btnChat = btnChat;
    this.btnExplore = btnExplore;
    this.btnNotification = btnNotification;
    this.btnOrder = btnOrder;
    this.buttonRow = buttonRow;
    this.cardAction = cardAction;
    this.cardChat = cardChat;
    this.cardImage = cardImage;
    this.cardNotification = cardNotification;
    this.cardOrder = cardOrder;
    this.descriptionCard = descriptionCard;
    this.descriptionText = descriptionText;
    this.imageView = imageView;
  }

  @Override
  @NonNull
  public RelativeLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentMessageBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentMessageBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_message, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentMessageBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btnChat;
      Button btnChat = ViewBindings.findChildViewById(rootView, id);
      if (btnChat == null) {
        break missingId;
      }

      id = R.id.btnExplore;
      Button btnExplore = ViewBindings.findChildViewById(rootView, id);
      if (btnExplore == null) {
        break missingId;
      }

      id = R.id.btnNotification;
      Button btnNotification = ViewBindings.findChildViewById(rootView, id);
      if (btnNotification == null) {
        break missingId;
      }

      id = R.id.btnOrder;
      Button btnOrder = ViewBindings.findChildViewById(rootView, id);
      if (btnOrder == null) {
        break missingId;
      }

      id = R.id.buttonRow;
      LinearLayout buttonRow = ViewBindings.findChildViewById(rootView, id);
      if (buttonRow == null) {
        break missingId;
      }

      id = R.id.cardAction;
      CardView cardAction = ViewBindings.findChildViewById(rootView, id);
      if (cardAction == null) {
        break missingId;
      }

      id = R.id.cardChat;
      CardView cardChat = ViewBindings.findChildViewById(rootView, id);
      if (cardChat == null) {
        break missingId;
      }

      id = R.id.cardImage;
      CardView cardImage = ViewBindings.findChildViewById(rootView, id);
      if (cardImage == null) {
        break missingId;
      }

      id = R.id.cardNotification;
      CardView cardNotification = ViewBindings.findChildViewById(rootView, id);
      if (cardNotification == null) {
        break missingId;
      }

      id = R.id.cardOrder;
      CardView cardOrder = ViewBindings.findChildViewById(rootView, id);
      if (cardOrder == null) {
        break missingId;
      }

      id = R.id.descriptionCard;
      CardView descriptionCard = ViewBindings.findChildViewById(rootView, id);
      if (descriptionCard == null) {
        break missingId;
      }

      id = R.id.descriptionText;
      TextView descriptionText = ViewBindings.findChildViewById(rootView, id);
      if (descriptionText == null) {
        break missingId;
      }

      id = R.id.imageView;
      ImageView imageView = ViewBindings.findChildViewById(rootView, id);
      if (imageView == null) {
        break missingId;
      }

      return new FragmentMessageBinding((RelativeLayout) rootView, btnChat, btnExplore,
          btnNotification, btnOrder, buttonRow, cardAction, cardChat, cardImage, cardNotification,
          cardOrder, descriptionCard, descriptionText, imageView);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
