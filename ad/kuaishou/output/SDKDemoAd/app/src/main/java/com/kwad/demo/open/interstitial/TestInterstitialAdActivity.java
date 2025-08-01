package com.kwad.demo.open.interstitial;

import static com.kwad.demo.open.feed.FeedHomeActivity.POS_ID;

import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.kwad.demo.R;
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.serverBid.BiddingDemoUtils;
import com.kwad.demo.open.utils.LogUtils;
import com.kwad.demo.open.utils.TestSpUtil;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.sdk.api.KsInterstitialAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsVideoPlayConfig;
import com.kwad.sdk.api.model.AdClickAction;
import com.kwad.sdk.api.model.AdExposureFailedReason;
import com.kwad.sdk.api.model.AdExposureFailureCode;
import com.kwad.sdk.api.model.AdShowAction;
import com.kwad.sdk.api.model.AdnName;
import com.kwad.sdk.api.model.AdnType;

// 建议将用于弹出插屏弹窗的Activity屏幕方向固定，使其不跟随系统自动旋转屏幕方向
public class TestInterstitialAdActivity extends Activity implements View.OnClickListener {
  private static final String TAG = "TestInterstitialAd";

  private Context mContext;
  private KsInterstitialAd mKsInterstitialAd;
  private View mBackBtn;
  private Switch mVideoSoundSwitch;
  private long mPosId;
  private Switch mChangeOrientation; //切换当前屏幕方向
  private boolean isAdRequesting = false;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    setContentView(R.layout.activity_test_interstitial);
    initView();
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POSID_INTERSTITIAL.posId);
  }

  private void initView() {
    mBackBtn = findViewById(R.id.ksad_main_left_back_btn);
    mBackBtn.setOnClickListener(this);
    mVideoSoundSwitch = findViewById(R.id.video_sound_switch);
    mChangeOrientation = findViewById(R.id.change_orientation_switch);
    setChangeOrientationListener();
  }


  // 1.请求插屏广告，获取广告对象，InterstitialAd
  public void requestInterstitialAd(View view) {
    if (isAdRequesting) {
      return;
    }
    isAdRequesting = true;
    mKsInterstitialAd = null;
    // 此为测试posId，请联系快手平台申请正式posId
    KsScene.Builder builder = KSSdkInitUtil.createKSSceneBuilder(mPosId);
    if (!TextUtils.isEmpty(mBidResponseV1)) { // 设置在服务端竞价后的广告信息
      builder.setBidResponse(mBidResponseV1);
    } else if (!TextUtils.isEmpty(mBidResponseV2)) {
      builder.setBidResponseV2(mBidResponseV2);
    }
    KSSdkInitUtil.getLoadManager().loadInterstitialAd(builder.build(),
        new KsLoadManager.InterstitialAdListener() {
          @Override
          public void onError(int code, String msg) {
            ToastUtil.showToast(mContext, "插屏广告请求失败" + code + msg);
            LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "loadInterstitialAd_onError");
            isAdRequesting = false;
          }

          @Override
          public void onRequestResult(int adNumber) {
            LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onRequestResult");
            ToastUtil.showToast(mContext, "插屏广告请求填充个数 " + adNumber);
          }


          @Override
          public void onInterstitialAdLoad(@Nullable List<KsInterstitialAd> adList) {
            isAdRequesting = false;
            if (adList != null && adList.size() > 0) {
              mKsInterstitialAd = adList.get(0);
              ToastUtil.showToast(mContext, "插屏广告请求成功");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onInterstitialAdLoad");
              KsVideoPlayConfig videoPlayConfig = new KsVideoPlayConfig.Builder()
                  .videoSoundEnable(!mVideoSoundSwitch.isChecked())
                  .build();
              showInterstitialAd(videoPlayConfig);
            }
          }
        });
  }

  private void showInterstitialAd(KsVideoPlayConfig videoPlayConfig) {
    if (mKsInterstitialAd != null) {
      mKsInterstitialAd
          .setAdInteractionListener(new KsInterstitialAd.AdInteractionListener() {
            @Override
            public void onAdClicked() {
              ToastUtil.showToast(mContext, "插屏广告点击");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onAdClicked");
            }

            @Override
            public void onAdShow() {
              ToastUtil.showToast(mContext, "插屏广告曝光");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onAdShow");
            }

            @Override
            public void onAdClosed() {
              ToastUtil.showToast(mContext, "用户点击插屏关闭按钮");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onAdClosed");
            }

            @Override
            public void onPageDismiss() {
              Log.i(TAG, "插屏广告关闭");
              ToastUtil.showToast(mContext, "插屏广告关闭");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onPageDismiss");
            }

            @Override
            public void onVideoPlayError(int code, int extra) {
              ToastUtil.showToast(mContext, "插屏广告播放出错");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onVideoPlayError");
            }

            @Override
            public void onVideoPlayEnd() {
              ToastUtil.showToast(mContext, "插屏广告播放完成");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onVideoPlayEnd");
            }

            @Override
            public void onVideoPlayStart() {
              ToastUtil.showToast(mContext, "插屏广告播放开始");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onVideoPlayStart");
            }

            @Override
            public void onSkippedAd() {
              ToastUtil.showToast(mContext, "插屏广告播放跳过");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onSkippedAd");
            }

          });
      mKsInterstitialAd.showInterstitialAd(this, videoPlayConfig);
    } else {
      ToastUtil.showToast(mContext, "暂无可用插屏广告，请等待缓存加载或者重新刷新");
    }
  }

  private void setChangeOrientationListener() {
    mChangeOrientation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setRequestedOrientation(isChecked ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      }
    });
  }

  /********   服务端竞价模拟 start   ********/
  private String mBidResponseV1;
  private String mBidResponseV2;

  // 获取服务端竞价信息
  public void fetchBidResponseV1(View view) {
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(mPosId).build(); // 根据需要传入场景参数，需传入有效posId
    String token = KSSdkInitUtil.getLoadManager().getBidRequestToken(ksScene);
    BiddingDemoUtils.fetchBidResponse(this,mPosId, token, false,
            new BiddingDemoUtils.FetchResponseCallback() {
              @Override
              public void onSuccess(String bidResponse) {
                mBidResponseV1 = bidResponse;
              }
            });
  }

  public void showAdV1(View view) {
    if (TextUtils.isEmpty(mBidResponseV1)) {
      ToastUtil.showToast(mContext, "请先获取竞价信息");
      return;
    }
    requestInterstitialAd(view);
  }

  public void fetchBidResponseV2(View view) {
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(mPosId).build(); // 根据需要传入场景参数，注意：创建KsScene时 posId 可传无效值，在adx服务端拉取快手竞价信息时必须传有效的 posId
    String token = KSSdkInitUtil.getLoadManager().getBidRequestTokenV2(ksScene);
    BiddingDemoUtils.fetchBidResponse(this, mPosId, token, true,
            new BiddingDemoUtils.FetchResponseCallback() {
              @Override
              public void onSuccess(String bidResponse) {
                mBidResponseV2 = bidResponse;
              }
            });
  }

  public void showAdV2(View view) {
    if (TextUtils.isEmpty(mBidResponseV2)) {
      ToastUtil.showToast(mContext, "请先获取竞价信息");
      return;
    }
    requestInterstitialAd(view);
  }

  /********   服务端竞价模拟 end     ********/

  @Override
  public void onClick(View v) {
    if (v == mBackBtn) {
      finish();
    }
  }
}
