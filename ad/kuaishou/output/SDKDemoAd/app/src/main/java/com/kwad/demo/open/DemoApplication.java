package com.kwad.demo.open;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.Nullable;
import android.webkit.WebView;
import androidx.multidex.MultiDex;

import com.kwad.demo.BuildConfig;
import com.kwad.demo.open.utils.SystemUtil;
import com.kwad.demo.open.utils.TestPermissionUtil;

public class DemoApplication extends Application {
  @SuppressLint("StaticFieldLeak")
  public static Application sContext;

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    MultiDex.install(this);
  }

  @Override
  public void onCreate() {
    sContext = this;

    // 强烈建议在Application#onCreate()方法中调用，避免出现context为null的异常
    if (SystemUtil.isMainProcess(this)) {
      // 建议只在需要的进程初始化SDK即可，如主进程
      // 是否获取关于隐私协议及SDK使用规范的授权
      if (TestPermissionUtil.isUserAgreePrivacy()) {
        KSSdkInitUtil.initSDK(this);
      }
    }
    super.onCreate();

  }
}