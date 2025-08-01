package com.kwad.demo.open.utils;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.kwad.demo.BuildConfig;
import com.kwad.demo.R;

public class ToastUtil {
  private static final String TAG = "快手联盟Toast";

  public static void showToast(Context context, String msg) {
    if (!BuildConfig.isToastEnable) {
      return;
    }
    try {
      Log.w(TAG, Thread.currentThread().getName() + ", " + msg);
      TextView view = new TextView(context.getApplicationContext());
      ViewGroup.LayoutParams params =
          new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
              ViewGroup.LayoutParams.WRAP_CONTENT);
      view.setLayoutParams(params);
      view.setText(msg);
      view.setTextColor(context.getResources().getColor(R.color.ksad_white));
      view.setBackgroundResource(R.drawable.test_toast_background);
      view.setTextSize(16);
      int topBottomPadding = ViewUtil.dip2px(context, 13);
      int leftRightPadding = ViewUtil.dip2px(context, 20);
      view.setPadding(leftRightPadding, topBottomPadding, leftRightPadding, topBottomPadding);
      Toast toast = new Toast(context.getApplicationContext());
      toast.setGravity(Gravity.CENTER, 0, 0);
      toast.setDuration(Toast.LENGTH_SHORT);
      toast.setView(view);
      toast.show();
    } catch (Throwable throwable) {
      //ignore
    }
  }

}
