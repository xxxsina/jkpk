package com.kwad.demo.open.splash;

import static com.kwad.demo.open.feed.FeedHomeActivity.POS_ID;

import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.kwad.demo.R;
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.MainActivity;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.utils.LogUtils;
import com.kwad.demo.open.utils.TestPermissionUtil;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsSplashScreenAd;
import com.kwad.sdk.api.model.AdClickAction;
import com.kwad.sdk.api.model.AdExposureFailedReason;
import com.kwad.sdk.api.model.AdExposureFailureCode;
import com.kwad.sdk.api.model.AdShowAction;
import com.kwad.sdk.api.model.AdnName;
import com.kwad.sdk.api.model.AdnType;

public class TestSplashScreenViewActivity extends AppCompatActivity {
  private static final String TAG = "splash_test";
  public static final String KEY_GO_TO_MAIN = "KEY_GO_TO_MAIN";
  public static final String KEY_BID_RESPONSE = "KEY_BID_RESPONSE";
  public static final String KEY_BID_RESPONSE_V2 = "KEY_BID_RESPONSE_V2";
  public static final String KEY_IS_LANDSCAPE = "KEY_IS_LANDSCAPE";

  private ViewGroup mEmptyView;
  private ViewGroup mSplashAdContainer;
  private boolean mIsPaused;
  private boolean mGotoMainActivity;
  private boolean mIsResumeGoToMain;
  private String mBidResponse;
  private String mBidResponseV2;
  private long mPosId;
  private Context mContext;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    setContentView(R.layout.activity_test_splash_screen);
    mIsResumeGoToMain = getIntent().getBooleanExtra(KEY_GO_TO_MAIN, false);
    mBidResponse = getIntent().getStringExtra(KEY_BID_RESPONSE);
    mBidResponseV2 = getIntent().getStringExtra(KEY_BID_RESPONSE_V2);
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POSID_SPLASHSCREEN.posId);
    mEmptyView = findViewById(R.id.splash_ad_empty);
    mSplashAdContainer = findViewById(R.id.splash_ad_container);

    mEmptyView.setVisibility(View.VISIBLE);
    mSplashAdContainer.setVisibility(View.GONE);

    int isLandscape = getIntent().getIntExtra(KEY_IS_LANDSCAPE, -1);
    if (isLandscape != -1) {
      // 横屏时设置为横屏的id
      mPosId = TestPosId.POSID_SPLASHSCREEN_LANDSCAPE.posId;
      setRequestedOrientation(isLandscape == 1 ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
          : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    // 是否获取关于隐私协议及SDK使用规范的授权
    if (TestPermissionUtil.isUserAgreePrivacy()) {
      requestSplashScreenAd();
    } else {
      gotoMainActivity();
    }
  }

  // 1.请求开屏广告，获取广告对象，KsFullScreenVideoAd
  public void requestSplashScreenAd() {
    KsScene.Builder builder =
        KSSdkInitUtil.createKSSceneBuilder(mPosId); // 此为测试posId，请联系快手平台申请正式posId

    if (builder == null) {
      // 创建失败，直接进入首页
      gotoMainActivity();
      return;
    }

    if (!TextUtils.isEmpty(mBidResponse)) {  // 服务端竞价
      builder.setBidResponse(mBidResponse);
    } else if (!TextUtils.isEmpty(mBidResponseV2)) {
      builder.setBidResponseV2(mBidResponseV2);
    }
    KsScene scene = builder.build();
    KsAdSDK.getLoadManager().loadSplashScreenAd(scene, new KsLoadManager.SplashScreenAdListener() {
      @Override
      public void onError(int code, String msg) {
        mSplashAdContainer.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.VISIBLE);
        showTips("开屏广告请求失败" + code + msg);
        LogUtils.recodeCallback(LogUtils.SCENE_SPLASH_PRE, "loadSplashScreenAd_onError");
        gotoMainActivity();
      }

      @Override
      public void onRequestResult(int adNumber) {
        LogUtils.recodeCallback(LogUtils.SCENE_SPLASH_PRE, "onRequestResult");
        showTips("开屏广告广告填充" + adNumber);
      }

      @Override
      public void onSplashScreenAdLoad(@NonNull KsSplashScreenAd splashScreenAd) {
        mSplashAdContainer.setVisibility(View.VISIBLE);
        showTips("开始数据返回成功");
        LogUtils.recodeCallback(LogUtils.SCENE_SPLASH_PRE, "onSplashScreenAdLoad");
        addView(splashScreenAd);
      }
    });
  }

  private void addView(final KsSplashScreenAd splashScreenAd) {
    if (isFinishing()) {
      return;
    }
    View view = splashScreenAd.getView(mContext, new KsSplashScreenAd.SplashScreenAdInteractionListener() {
      @Override
      public void onAdClicked() {
        showTips("开屏广告点击");
        LogUtils.recodeCallback(LogUtils.SCENE_SPLASH_PRE, "onAdClicked");
        /**
         * 开屏广告点击会吊起h5或应用商店，并回调onAdClick(), mGotoMainActivity控制由h5或应用商店返回后是否直接进入主界面
         */
        mGotoMainActivity = mIsResumeGoToMain;
      }

      @Override
      public void onAdShowError(int code, String extra) {
        showTips("开屏广告显示错误 " + code + " extra " + extra);
        LogUtils.recodeCallback(LogUtils.SCENE_SPLASH_PRE, "onAdShowError");
        //出错不触发显示miniWindow
        gotoMainActivity();
      }

      @Override
      public void onAdShowEnd() {
        showTips("开屏广告显示结束");
        LogUtils.recodeCallback(LogUtils.SCENE_SPLASH_PRE, "onAdShowEnd");
        gotoMainActivity();
      }

      @Override
      public void onAdShowStart() {
        showTips("开屏广告显示开始");
        LogUtils.recodeCallback(LogUtils.SCENE_SPLASH_PRE, "onAdShowStart");
        mEmptyView.setVisibility(View.GONE);
      }

      @Override
      public void onSkippedAd() {
        showTips("用户跳过开屏广告");
        LogUtils.recodeCallback(LogUtils.SCENE_SPLASH_PRE, "onSkippedAd");
        gotoMainActivity();
      }

      @Override
      public void onDownloadTipsDialogShow() {
        showTips("开屏广告显示下载合规弹窗");
        LogUtils.recodeCallback(LogUtils.SCENE_SPLASH_PRE, "onDownloadTipsDialogShow");
      }

      @Override
      public void onDownloadTipsDialogDismiss() {
        showTips("开屏广告关闭下载合规弹窗");
        LogUtils.recodeCallback(LogUtils.SCENE_SPLASH_PRE, "onDownloadTipsDialogDismiss");
      }

      @Override
      public void onDownloadTipsDialogCancel() {
        showTips("开屏广告取消下载合规弹窗");
        LogUtils.recodeCallback(LogUtils.SCENE_SPLASH_PRE, "onDownloadTipsDialogCancel");
      }
    });
    if (view == null) {
      gotoMainActivity();
    } else {
      ViewGroup root = findViewById(R.id.splash_ad_container);
      root.removeAllViews();
      view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT));
      root.addView(view);
    }
  }



  void showTips(String msg) {
    ToastUtil.showToast(this, msg);
    Log.d(TAG, "showTips " + msg);
  }

  @Override
  protected void onPause() {
    super.onPause();
    mIsPaused = true;
  }

  @Override
  protected void onResume() {
    super.onResume();
    mIsPaused = false;
    if (mGotoMainActivity) {
      gotoMainActivity();
    }
  }

  @Override
  public void finish() {
    super.finish();
  }

  private void gotoMainActivity() {
    if (mIsPaused) {
      mGotoMainActivity = true;
    } else {
      Intent intent = new Intent(TestSplashScreenViewActivity.this, MainActivity.class);
      startActivity(intent);
      overridePendingTransition(0, 0);
      finish();
    }
  }

}
