// Generated by view binder compiler. Do not edit!
package com.example.fyp.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

public final class ItemRecommendedClothingBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final ImageView imageViewClothingR;

  @NonNull
  public final TextView textViewClothingNameR;

  @NonNull
  public final TextView textViewClothingPriceR;

  private ItemRecommendedClothingBinding(@NonNull LinearLayout rootView,
      @NonNull ImageView imageViewClothingR, @NonNull TextView textViewClothingNameR,
      @NonNull TextView textViewClothingPriceR) {
    this.rootView = rootView;
    this.imageViewClothingR = imageViewClothingR;
    this.textViewClothingNameR = textViewClothingNameR;
    this.textViewClothingPriceR = textViewClothingPriceR;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ItemRecommendedClothingBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ItemRecommendedClothingBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.item_recommended_clothing, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ItemRecommendedClothingBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.imageViewClothingR;
      ImageView imageViewClothingR = ViewBindings.findChildViewById(rootView, id);
      if (imageViewClothingR == null) {
        break missingId;
      }

      id = R.id.textViewClothingNameR;
      TextView textViewClothingNameR = ViewBindings.findChildViewById(rootView, id);
      if (textViewClothingNameR == null) {
        break missingId;
      }

      id = R.id.textViewClothingPriceR;
      TextView textViewClothingPriceR = ViewBindings.findChildViewById(rootView, id);
      if (textViewClothingPriceR == null) {
        break missingId;
      }

      return new ItemRecommendedClothingBinding((LinearLayout) rootView, imageViewClothingR,
          textViewClothingNameR, textViewClothingPriceR);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
