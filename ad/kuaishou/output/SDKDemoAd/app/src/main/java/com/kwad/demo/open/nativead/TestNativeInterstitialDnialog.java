package com.kwad.demo.open.nativead;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.kwad.demo.R;

public class TestNativeInterstitialDnialog extends Dialog {

  private ImageView button;

  public TestNativeInterstitialDnialog(Context context, View view) {
    super(context);
    setContentView(view);
    button = view.findViewById(R.id.ad_dislike);
// 设置宽度和高度
    WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
    layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT; // 设置宽度为全屏
    layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT; // 设置高度为自适应内容
    getWindow().setAttributes(layoutParams);
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dismiss();
      }
    });
  }
}
