package com.kwad.demo.open.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.PrivilegeDialog.PrivacyDialogFragment;
import com.kwad.demo.open.UserDataObtainController;

public class TestPermissionUtil {
  private static final String KEY_USER_AGREE_PRIVACY = "user_agree_privacy";

  private static boolean isPermissionGranted(Activity activity, String... permissions) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return true;
    }
    for (String permission : permissions) {
      if (ContextCompat.checkSelfPermission(activity, permission) ==
          PackageManager.PERMISSION_DENIED) {
        return false;
      }
    }
    return true;
  }

  private static void requestPermission(Activity activity, String... permissions) {
    if (!isPermissionGranted(activity, permissions)) {
      ActivityCompat.requestPermissions(activity, permissions, 800);
    }
  }

  private static void requestPermissionIfNeed(Activity activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return;
    }
    if (Build.VERSION.SDK_INT >= 33) {
      requestPermission(activity, Manifest.permission.READ_PHONE_STATE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.CHANGE_NETWORK_STATE,
          "android.permission.POST_NOTIFICATIONS");
    } else {
      requestPermission(activity, Manifest.permission.READ_PHONE_STATE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.CHANGE_NETWORK_STATE);
    }
  }

  public static void handlePermission(final Activity activity) {
    // 隐私政策及使用规范
    if (isUserAgreePrivacy()) {
      requestPermission(activity);
      return;
    }
    PrivacyDialogFragment.showPrivacyDialog(activity,
        new PrivacyDialogFragment.PrivacyReadStateListener() {
          @Override
          public void onNotAccept() {
            activity.finish();
          }

          @Override
          public void onAccept() {
            TestSpUtil.savaBoolean(KEY_USER_AGREE_PRIVACY, true);
            UserDataObtainController.getInstance().setUserAgree(true);
            requestPermissionIfNeed(activity);
            KSSdkInitUtil.initSDK(activity);
          }
        });
  }

  public static boolean isUserAgreePrivacy() {
    return TestSpUtil.getBoolean(TestPermissionUtil.KEY_USER_AGREE_PRIVACY);
  }
}
