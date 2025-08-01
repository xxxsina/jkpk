package com.kwad.demo.open;

import java.lang.reflect.Field;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.kwad.demo.R;
import com.kwad.demo.open.banner.TestBannerAdActivity;
import com.kwad.demo.open.draw.TestDrawVideoActivity;
import com.kwad.demo.open.feed.FeedHomeActivity;
import com.kwad.demo.open.fullscreen.TestFullScreenVideoActivity;
import com.kwad.demo.open.interstitial.TestInterstitialAdActivity;
import com.kwad.demo.open.interstitial.TestNewInterstitialAdActivity;
import com.kwad.demo.open.nativead.TestNativeAdActivity;
import com.kwad.demo.open.reward.TestRewardVideoActivity;
import com.kwad.demo.open.splash.SplashHomeActivity;
import com.kwad.demo.open.utils.TestPermissionUtil;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.devTools.ToolsActivity;
import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsExitInstallListener;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.core.IKsAdSDK;
import com.kwad.sdk.api.loader.Loader;


public class MainActivity extends Activity {
  private final static String TAG = "MainActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // 建议在获取广告前申请SDK需要权限，特别是IMIE权限。
    TestPermissionUtil.handlePermission(this);

    TextView sdkVersion = findViewById(R.id.sdk_version);
    sdkVersion.setText(String.format("SDK Version %s  ", KsAdSDK.getSDKVersion()));
    final TextView sdkDid = findViewById(R.id.sdk_did);
    sdkDid.setText(String.format("DeviceId: %s  ", KsAdSDK.getDid()));

  }


  public void startSDK(View view) {
    KsAdSDK.start();
  }

  public void testFullscreen(View view) {
    Intent intent = new Intent(MainActivity.this, TestFullScreenVideoActivity.class);
    startActivity(intent);
  }

  public void testReward(View view) {
    Intent intent = new Intent(MainActivity.this, TestRewardVideoActivity.class);
    startActivity(intent);
  }

  public void testFeedList(View view) {
    Intent intent = new Intent(MainActivity.this, FeedHomeActivity.class);
    startActivity(intent);
  }

  public void testDrawAd(View view) {
    Intent intent = new Intent(MainActivity.this, TestDrawVideoActivity.class);
    startActivity(intent);
  }

  public void testSplash(View view) {
    Intent intent = new Intent(MainActivity.this, SplashHomeActivity.class);
    startActivity(intent);
  }

  public void testBannerAd(View view) {
    Intent intent = new Intent(MainActivity.this, TestBannerAdActivity.class);
    startActivity(intent);
  }

  public void testNativeAd(View view) {
    Intent intent = new Intent(MainActivity.this, TestNativeAdActivity.class);
    startActivity(intent);
  }

  public void testInterstitialAd(View view) {
    Intent intent = new Intent(MainActivity.this, TestInterstitialAdActivity.class);
    startActivity(intent);
  }

  public void testNewInterstitialAd(View view) {
    Intent intent = new Intent(MainActivity.this, TestNewInterstitialAdActivity.class);
    startActivity(intent);
  }

  public void openDevTools(View view) {
    try {
      Intent intent = new Intent(MainActivity.this, ToolsActivity.class);
      startActivity(intent);
    } catch (Throwable e) {
      ToastUtil.showToast(MainActivity.this, "调试工具不可用");
    }
  }

  @Override
  public void onBackPressed() {
    boolean isShowInstallDialog = tryShowInstallDialog();
    if (isShowInstallDialog) {
      return;
    }
    super.onBackPressed();
  }

  /**
   * 退出app时，尝试展示广告安装提升弹框
   */
  private boolean tryShowInstallDialog() {
    KsLoadManager loadManager = KSSdkInitUtil.getLoadManager();
    //尝试展示安装提升弹框，有未安装的广告会展示弹框，弹框展示返回true，否则返回false
    return loadManager.showInstallDialog(this, new KsExitInstallListener() {

      @Override
      public void onInstallClick() {
        ToastUtil.showToast(MainActivity.this, "点击安装按钮");
      }

      @Override
      public void onDialogClose() {
        ToastUtil.showToast(MainActivity.this, "安装弹框关闭");
        //退出页面
        finish();
      }
    });
  }
}
