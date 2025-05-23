// Generated by view binder compiler. Do not edit!
package com.example.fyp.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.fyp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityAdminProductListBinding implements ViewBinding {
  @NonNull
  private final RelativeLayout rootView;

  @NonNull
  public final FloatingActionButton btnAddProduct;

  @NonNull
  public final RecyclerView recyclerViewProducts;

  @NonNull
  public final TextView tvHeading;

  @NonNull
  public final TextView tvNoProducts;

  private ActivityAdminProductListBinding(@NonNull RelativeLayout rootView,
      @NonNull FloatingActionButton btnAddProduct, @NonNull RecyclerView recyclerViewProducts,
      @NonNull TextView tvHeading, @NonNull TextView tvNoProducts) {
    this.rootView = rootView;
    this.btnAddProduct = btnAddProduct;
    this.recyclerViewProducts = recyclerViewProducts;
    this.tvHeading = tvHeading;
    this.tvNoProducts = tvNoProducts;
  }

  @Override
  @NonNull
  public RelativeLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityAdminProductListBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityAdminProductListBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_admin_product_list, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityAdminProductListBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btnAddProduct;
      FloatingActionButton btnAddProduct = ViewBindings.findChildViewById(rootView, id);
      if (btnAddProduct == null) {
        break missingId;
      }

      id = R.id.recyclerViewProducts;
      RecyclerView recyclerViewProducts = ViewBindings.findChildViewById(rootView, id);
      if (recyclerViewProducts == null) {
        break missingId;
      }

      id = R.id.tvHeading;
      TextView tvHeading = ViewBindings.findChildViewById(rootView, id);
      if (tvHeading == null) {
        break missingId;
      }

      id = R.id.tvNoProducts;
      TextView tvNoProducts = ViewBindings.findChildViewById(rootView, id);
      if (tvNoProducts == null) {
        break missingId;
      }

      return new ActivityAdminProductListBinding((RelativeLayout) rootView, btnAddProduct,
          recyclerViewProducts, tvHeading, tvNoProducts);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
