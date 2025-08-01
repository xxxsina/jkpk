package com.kwad.demo.open.debug;

import android.app.Activity;
import android.app.Application;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.kwad.demo.R;

public class TestActivityListener {

  /**
   * 模拟Demo监听SDK的Activity生命周期
   */
  public static void testActivityLifecycle(Application application) {
    application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
      @Override
      public void onActivityCreated(@NonNull Activity activity,
          @Nullable Bundle savedInstanceState) {
        test(activity);
      }

      @Override
      public void onActivityStarted(@NonNull Activity activity) {
        test(activity);
      }

      @Override
      public void onActivityResumed(@NonNull Activity activity) {
        test(activity);
      }

      @Override
      public void onActivityPaused(@NonNull Activity activity) {
        test(activity);
      }

      @Override
      public void onActivityStopped(@NonNull Activity activity) {
        test(activity);
      }

      @Override
      public void onActivitySaveInstanceState(@NonNull Activity activity,
          @NonNull Bundle outState) {
      }

      @Override
      public void onActivityDestroyed(@NonNull Activity activity) {

      }
    });
  }

  private static void test(Activity activity) {
    String className = activity.getClass().getName();
    Log.w("TestActivityListener", "activity: " + className);
    if (className.contains("com.kwad.sdk")) {
      // SDK内部页面
      return;
    }
    String appName = activity.getResources().getString(R.string.app_name);
    if (TextUtils.isEmpty(appName)) {
      throw new Resources.NotFoundException("String resource ID #0x"
          + Integer.toHexString(R.string.app_name));
    }
  }
}
