package com.kwad.demo.open.banner;

import static com.kwad.demo.open.feed.FeedHomeActivity.POS_ID;
import static com.kwad.sdk.api.KsAdSDK.getContext;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.kwad.demo.R;
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.feed.TestConfigFeedListActivity;
import com.kwad.demo.open.interstitial.TestInterstitialAdActivity;
import com.kwad.demo.open.reward.TestRewardVideoActivity;
import com.kwad.demo.open.serverBid.BiddingDemoUtils;
import com.kwad.demo.open.utils.LogUtils;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.sdk.api.KsAdVideoPlayConfig;
import com.kwad.sdk.api.KsBannerAd;
import com.kwad.sdk.api.KsFeedAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsVideoPlayConfig;
import com.kwad.sdk.api.SdkConfig;
import com.kwad.sdk.api.model.AdClickAction;
import com.kwad.sdk.api.model.AdExposureFailedReason;
import com.kwad.sdk.api.model.AdExposureFailureCode;
import com.kwad.sdk.api.model.AdShowAction;
import com.kwad.sdk.api.model.AdnName;
import com.kwad.sdk.api.model.AdnType;

public class TestBannerAdActivity extends Activity {
  private static final String TAG = "TestBannerAdActivity";
  private Context mContext;
  private Switch mVoiceSwitch; //是否开启在线播放
  private Switch mShowLandscapeSwitch; //是否开启横屏展示
  private Long mPosId;

  private KsAdVideoPlayConfig videoPlayConfig;
  private KsBannerAd mKsBannerAd;
  private Button mButton1;
  private Button mButton2;
  private Button mButton3;
  private Button mButton4;
  //模拟展示的是否是快手广告
  private boolean mIsBidFailed = false;
  private final KsBannerAd.BannerAdInteractionListener mBannerAdInteractionListener =
      new KsBannerAd.BannerAdInteractionListener() {
        @Override
        public void onAdClicked() {
          ToastUtil.showToast(TestBannerAdActivity.this, "广告点击");
          LogUtils.recodeCallback(LogUtils.SCENE_BANNER_PRE, "onAdClicked");
          if (mIsBidFailed && mKsBannerAd != null) {
            //模拟竞价失败，展示其他广告时点击上报
            testBidFailedClickAndShowReport(mKsBannerAd, AdActionType.CLICK);
          }
        }

        @Override
        public void onAdShow() {
          ToastUtil.showToast(TestBannerAdActivity.this, "广告展示");
          LogUtils.recodeCallback(LogUtils.SCENE_BANNER_PRE, "onAdShow");
          if (mIsBidFailed && mKsBannerAd != null) {
            //模拟竞价失败，展示其他广告时点击上报
            testBidFailedClickAndShowReport(mKsBannerAd, AdActionType.SHOW);
          }
        }

        @Override
        public void onAdClose() {
          ToastUtil.showToast(TestBannerAdActivity.this, "广告关闭");
          LogUtils.recodeCallback(LogUtils.SCENE_BANNER_PRE, "onAdClose");
        }

        @Override
        public void onAdShowError(int i, String onMediaPlayError) {
          ToastUtil.showToast(TestBannerAdActivity.this, "展示错误");
          LogUtils.recodeCallback(LogUtils.SCENE_BANNER_PRE, "onAdShowError");
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POS_ID_BANNER_1.posId);
    setContentView(R.layout.activity_test_banner_ad);
    initView();
  }

  private void initView() {
    mVoiceSwitch = findViewById(R.id.switch_banner_voice);
    mShowLandscapeSwitch = findViewById(R.id.show_banner_landscape_switch);
    mButton1 = findViewById(R.id.ksad_banner_type_1);
    mButton2 = findViewById(R.id.ksad_banner_type_2);
    mButton3 = findViewById(R.id.ksad_banner_type_3);
    mButton4 = findViewById(R.id.ksad_banner_type_4);
    setChangeOrientationListener();
    setVoiceSwitch();

  }

  private void setChangeOrientationListener() {
    mShowLandscapeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setRequestedOrientation(isChecked ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      }
    });
  }

  private void setVoiceSwitch() {
    videoPlayConfig = new KsAdVideoPlayConfig.Builder()
        .build();
    mVoiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        videoPlayConfig.setVideoSoundEnable(isChecked);
      }
    });
  }

  public void requestBannerAd(View view) {
    //视频展示的屏幕方向，建议和当前屏幕方向一致
    int screenOrientation = getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ?
        SdkConfig.SCREEN_ORIENTATION_PORTRAIT : SdkConfig.SCREEN_ORIENTATION_LANDSCAPE;

    //构建请求参数，此为测试posId，请联系快手平台申请正式posId
    KsScene.Builder builder = KSSdkInitUtil.createKSSceneBuilder(mPosId)
        .screenOrientation(screenOrientation);
    if (!TextUtils.isEmpty(mBidResponseV1)) { // 设置在服务端竞价后的广告信息
      builder.setBidResponse(mBidResponseV1);
    } else if (!TextUtils.isEmpty(mBidResponseV2)) {
      builder.setBidResponseV2(mBidResponseV2);
    }
    KsScene scene = builder.build();
    KSSdkInitUtil.getLoadManager().loadBannerAd(scene, new KsLoadManager.BannerAdListener() {
      @Override
      public void onError(int code, String msg) {
        LogUtils.recodeCallback(LogUtils.SCENE_BANNER_PRE, "onError");
        ToastUtil.showToast(TestBannerAdActivity.this, msg);
      }

      @Override
      public void onBannerAdLoad(@Nullable KsBannerAd bannerAd) {
        LogUtils.recodeCallback(LogUtils.SCENE_BANNER_PRE, "onBannerAdLoad");
        assert bannerAd != null;
        View view = bannerAd.getView(mContext, mBannerAdInteractionListener, videoPlayConfig);
        /**********DevelopCode Start***********/
        // 模拟竞价
        mKsBannerAd = bannerAd;
        testBiddingReport(bannerAd);
        /**********DevelopCode End***********/
        if (view != null) {
          ViewGroup root = findViewById(R.id.ksad_banner_content);
          root.removeAllViews();
          root.addView(view);
        }
      }
    });
  }

  public void onBackClick(View view) {
    onBackPressed();
  }

  /********   服务端竞价模拟 start   ********/
  private String mBidResponseV1;
  private String mBidResponseV2;

  // 获取服务端竞价信息
  public void fetchBidResponseV1(View view) {
    // 根据需要传入场景参数，需传入有效posId
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(mPosId)
        .build();
    String token = KSSdkInitUtil.getLoadManager().getBidRequestToken(ksScene);
    BiddingDemoUtils.fetchBidResponse(this, mPosId, token, false,
        new BiddingDemoUtils.FetchResponseCallback() {
          @Override
          public void onSuccess(String bidResponse) {
            mBidResponseV1 = bidResponse;
          }
        });
  }

  public void showAdV1(View view) {
    if (TextUtils.isEmpty(mBidResponseV1)) {
      ToastUtil.showToast(TestBannerAdActivity.this, "请先获取竞价信息");
      return;
    }
    requestBannerAd(view);
  }

  public void fetchBidResponseV2(View view) {
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(0)
        .build(); // 根据需要传入场景参数，注意：创建KsScene时 posId 可传无效值，在adx服务端拉取快手竞价信息时必须传有效的 posId
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
      ToastUtil.showToast(TestBannerAdActivity.this, "请先获取竞价信息");
      return;
    }
    requestBannerAd(view);
  }

  public void setSize1(View view) {
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POS_ID_BANNER_1.posId);
    updateButtonStatus(view);
  }

  public void setSize2(View view) {
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POS_ID_BANNER.posId);
    updateButtonStatus(view);
  }

  public void setSize3(View view) {
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POS_ID_BANNER_3.posId);
    updateButtonStatus(view);
  }

  public void setSize4(View view) {
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POS_ID_BANNER_2.posId);
    updateButtonStatus(view);
  }

  private void updateButtonStatus(View view) {
    if (view == mButton1) {
      mButton1.setTextColor(getContext().getResources().getColor(R.color.color_base_red_15));
      mButton2.setTextColor(getContext().getResources().getColor(R.color.black));
      mButton3.setTextColor(getContext().getResources().getColor(R.color.black));
      mButton4.setTextColor(getContext().getResources().getColor(R.color.black));
    } else if (view == mButton2) {
      mButton1.setTextColor(getContext().getResources().getColor(R.color.black));
      mButton2.setTextColor(getContext().getResources().getColor(R.color.color_base_red_15));
      mButton3.setTextColor(getContext().getResources().getColor(R.color.black));
      mButton4.setTextColor(getContext().getResources().getColor(R.color.black));
    } else if (view == mButton3) {
      mButton1.setTextColor(getContext().getResources().getColor(R.color.black));
      mButton2.setTextColor(getContext().getResources().getColor(R.color.black));
      mButton3.setTextColor(getContext().getResources().getColor(R.color.color_base_red_15));
      mButton4.setTextColor(getContext().getResources().getColor(R.color.black));
    } else if (view == mButton4) {
      mButton1.setTextColor(getContext().getResources().getColor(R.color.black));
      mButton2.setTextColor(getContext().getResources().getColor(R.color.black));
      mButton3.setTextColor(getContext().getResources().getColor(R.color.black));
      mButton4.setTextColor(getContext().getResources().getColor(R.color.color_base_red_15));
    }
  }

  public void testBiddingReport(KsBannerAd ksBannerAd) {
    LogUtils.recodeFuncLog(LogUtils.SCENE_BANNER_PRE,
        "getECPM() =" + ksBannerAd.getECPM());
    LogUtils.recodeFuncLog(LogUtils.SCENE_BANNER_PRE,
        "getMaterialType() =" + ksBannerAd.getMaterialType());
    LogUtils.recodeFuncLog(LogUtils.SCENE_BANNER_PRE,
        "getInteractionType() =" + ksBannerAd.getInteractionType());
    Random random = new Random();
    mIsBidFailed = false;
    if (random.nextBoolean()) {
      //模拟竞价成功
      int bidEcpm = random.nextInt(9999) + 1;
      int lossBidEcpm = random.nextInt(bidEcpm);
      LogUtils.recodeFuncLog(LogUtils.SCENE_BANNER_PRE,
          "setBidEcpm() =" + bidEcpm);
      ksBannerAd.setBidEcpm(bidEcpm);
      LogUtils.recodeFuncLog(LogUtils.SCENE_BANNER_PRE,
          "setBidEcpm() =" + (bidEcpm + "|" + lossBidEcpm));
      ksBannerAd.setBidEcpm(bidEcpm, lossBidEcpm);
    } else {
      //模拟竞价失败
      mIsBidFailed = true;
      @AdExposureFailureCode int failureCod = random.nextInt(5);
      AdExposureFailedReason reason = new AdExposureFailedReason();
      if (failureCod == AdExposureFailureCode.BID_FAILED) {
        int winEcpm = random.nextInt(10000);
        @AdnType int adnType = new Random().nextInt(3) + 1;
        reason.setWinEcpm(winEcpm)
            .setAdnType(adnType);
        if (adnType == AdnType.THIRD_PARTY_AD) {
          @AdnName String adnName;
          int rd = random.nextInt(4);
          if (rd == 0) {
            adnName = AdnName.CHUANSHANJIA;
          } else if (rd == 1) {
            adnName = AdnName.GUANGDIANTONG;
          } else if (rd == 2) {
            adnName = AdnName.BAIDU;
          } else {
            adnName = AdnName.OTHER;
          }
          reason.setAdnName(adnName);
        }
        reason.setAdnMaterialType(random.nextInt(7) + 1);
        reason.setAdnMaterialUrl("Banner 广告素材url-test");
      }
      ksBannerAd.reportAdExposureFailed(failureCod, reason);
    }
  }

  void testBidFailedClickAndShowReport(KsBannerAd ksBannerAd,
      AdActionType actionType) {
    AdExposureFailedReason reason = new AdExposureFailedReason();
    reason.setAdUserName("Banner 广告主名称-test");
    reason.setAdTitle("Banner 广告标题-test");
    reason.setAdRequestId("Banner 请求ID-test");
    reason.setAdnMaterialType(new Random().nextInt(7) + 1);
    reason.setAdnMaterialUrl("Banner 广告素材url-test");
    if (actionType == AdActionType.SHOW) {
      reason.setIsShow(AdShowAction.SHOW);
    } else if (actionType == AdActionType.CLICK) {
      reason.setIsClick(AdClickAction.CLICK);
    }
    ksBannerAd.reportAdExposureFailed(AdExposureFailureCode.BID_FAILED, reason);
  }

  public enum AdActionType {
    UNKNOWN,
    CLICK,
    SHOW
  }
  /********   服务端竞价模拟 end   ********/
}