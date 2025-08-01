package com.kwad.demo.open.interstitial;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
public class TestNewInterstitialAdActivity extends Activity implements View.OnClickListener {
  private static final String TAG = "TestInterstitialAd";

  private Context mContext;
  private KsInterstitialAd mKsInterstitialAd;
  private View mBackBtn;
  private Switch mVideoSoundSwitch;
  private long mPosId;
  private long mBiddingPosId;

  private Switch mChangeOrientation; //切换当前屏幕方向
  private boolean isAdRequesting = false;
  private MyInterLoadListener mLoadListener;

  /**********DevelopCode Start***********/
  //模拟展示的是否是快手广告
  private boolean mIsBidFailed = false;
  /**********DevelopCode End***********/

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    setContentView(R.layout.activity_test_new_interstitial);
    initView();

    /**********DevelopCode Start***********/
    int materialType = getIntent().getIntExtra("materialType", -1);
    int interactionType = getIntent().getIntExtra("interactionType", -1);
    int isLandscape = getIntent().getIntExtra("isLandscape", -1);
    int isMute = getIntent().getIntExtra("isMute", -1);
    if (isMute != -1) {
      mVideoSoundSwitch.setChecked(isMute != 0);
    }
    if (isLandscape != -1) {
      mChangeOrientation.setChecked(isLandscape == 1);
    }
    TestSpUtil.setInt(this, "materialType", materialType);
    TestSpUtil.setInt(this, "interactionType", interactionType);

    if (materialType != -1 || interactionType != -1 || isLandscape != -1 || isMute != -1) {
      KSSdkInitUtil.initSDK(this);
      requestNewInterstitialAd(null);
    }
    /**********DevelopCode End***********/
  }

  private void initView() {
    mBackBtn = findViewById(R.id.ksad_main_left_back_btn);
    mBackBtn.setOnClickListener(this);
    mVideoSoundSwitch = findViewById(R.id.video_sound_switch);
    mChangeOrientation = findViewById(R.id.change_orientation_switch);
    setChangeOrientationListener();

    RadioGroup radioGroup = findViewById(R.id.bidding_pos_group);
    radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {


      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        // 获取选中的 RadioButton 对象
        RadioButton selectedRadioButton = (RadioButton) group.findViewById(checkedId);

        if (selectedRadioButton != null) {
          // 获取选中项的文本
          String selectedOption = selectedRadioButton.getText().toString();

          // 根据选项执行相应操作
          switch (checkedId) {
            case R.id.bidding_posid_selected:
              // 优选被选中的情况
              mBiddingPosId = TestPosId.POS_ID_NEW_INTERSTITIAL.posId;
              break;
            case R.id.bidding_posid_full:
              // 全屏被选中的情况
              mBiddingPosId = TestPosId.POS_ID_NEW_INTERSTITIAL_FULL.posId;
              break;
            case R.id.bidding_posid_half:
              // 半屏被选中的情况
              mBiddingPosId = TestPosId.POS_ID_NEW_INTERSTITIAL_HALF.posId;
              break;
            default:
              // 默认优选
              mBiddingPosId = TestPosId.POS_ID_NEW_INTERSTITIAL.posId;
              break;
          }
        }
      }
    });
  }

  /**
   * 1.请求插屏广告，获取广告对象，InterstitialAd
   * 使用优选的广告位
   */
  public void requestNewInterstitialAd(View view) {
    mPosId = TestPosId.POS_ID_NEW_INTERSTITIAL.posId;
    doRequest();
  }

  /**
   * 1.请求插屏广告，获取广告对象，InterstitialAd
   * 使用全屏广告位
   */
  public void requestNewInterstitialAdFull(View view) {
    mPosId = TestPosId.POS_ID_NEW_INTERSTITIAL_FULL.posId;
    doRequest();
  }


  /**
   * 1.请求插屏广告，获取广告对象，InterstitialAd
   * 使用半屏的广告位
   */
  public void requestNewInterstitialAdHalf(View view) {
    mPosId = TestPosId.POS_ID_NEW_INTERSTITIAL_HALF.posId;
    doRequest();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mLoadListener != null) {
      mLoadListener.destroy();

      mLoadListener = null;
    }
  }

  private void doRequest() {
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

    mLoadListener = new MyInterLoadListener(this);
    KSSdkInitUtil.getLoadManager().loadInterstitialAd(builder.build(), mLoadListener);
  }

  private void showInterstitialAd(KsVideoPlayConfig videoPlayConfig) {
    if (mKsInterstitialAd != null) {
      /**********DevelopCode Start***********/
      // 模拟竞价
      testBiddingReport(mContext, mKsInterstitialAd);
      /**********DevelopCode End***********/
      mKsInterstitialAd
          .setAdInteractionListener(new KsInterstitialAd.AdInteractionListener() {
            @Override
            public void onAdClicked() {
              ToastUtil.showToast(mContext, "插屏广告点击");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onAdClicked");
              /**********DevelopCode Start***********/
              KsInterstitialAd tmpInterstitialAd = mKsInterstitialAd;
              if (mIsBidFailed && tmpInterstitialAd != null) {
                //模拟竞价失败，展示其他广告时点击上报
                testBidFailedClickAndShowReport(tmpInterstitialAd, AdActionType.CLICK);
              }
              /**********DevelopCode End***********/
            }

            @Override
            public void onAdShow() {
              ToastUtil.showToast(mContext, "插屏广告曝光");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onAdShow");
              /**********DevelopCode Start***********/
              KsInterstitialAd tmpInterstitialAd = mKsInterstitialAd;
              if (mIsBidFailed && tmpInterstitialAd != null) {
                //模拟竞价失败，展示其他广告时曝光上报
                testBidFailedClickAndShowReport(tmpInterstitialAd, AdActionType.SHOW);
              }
              /**********DevelopCode End***********/
            }

            @Override
            public void onAdClosed() {
              ToastUtil.showToast(mContext, "用户点击插屏关闭按钮");
              LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onAdClosed");
            }

            @Override
            public void onPageDismiss() {
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
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(mBiddingPosId).build(); // 根据需要传入场景参数，需传入有效posId
    String token = KSSdkInitUtil.getLoadManager().getBidRequestToken(ksScene);
    BiddingDemoUtils.fetchBidResponse(this,mBiddingPosId, token, false,
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
    requestNewInterstitialAd(view);
  }

  public void fetchBidResponseV2(View view) {
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(mBiddingPosId).build(); // 根据需要传入场景参数，注意：创建KsScene时 posId 可传无效值，在adx服务端拉取快手竞价信息时必须传有效的 posId
    String token = KSSdkInitUtil.getLoadManager().getBidRequestTokenV2(ksScene);
    BiddingDemoUtils.fetchBidResponse(this, mBiddingPosId, token, true,
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
    requestNewInterstitialAd(view);
  }

  /********   服务端竞价模拟 end     ********/
  /**********DevelopCode Start***********/
  private void testBiddingReport(Context context, KsInterstitialAd interstitialAd) {
    LogUtils.recodeFuncLog(LogUtils.SCENE_INSERT_PRE, "getECPM() =" + interstitialAd.getECPM());
    LogUtils.recodeFuncLog(LogUtils.SCENE_INSERT_PRE, "isVideo() =" + interstitialAd.isVideo());
    LogUtils.recodeFuncLog(LogUtils.SCENE_INSERT_PRE,
            "getMaterialType() =" + interstitialAd.getMaterialType());
    LogUtils.recodeFuncLog(LogUtils.SCENE_INSERT_PRE,
            "getInteractionType() =" + interstitialAd.getInteractionType());
    mIsBidFailed = false;
    Random random = new Random();
    if (random.nextBoolean()) {
      //模拟竞价成功
      int bidEcpm = random.nextInt(9999) + 1;
      int lossBidEcpm = random.nextInt(bidEcpm);
      LogUtils.recodeFuncLog(LogUtils.SCENE_INSERT_PRE,
              "setBidEcpm() =" + bidEcpm);
      interstitialAd.setBidEcpm(bidEcpm);
      LogUtils.recodeFuncLog(LogUtils.SCENE_INSERT_PRE,
              "setBidEcpm() =" + (bidEcpm + "|" + lossBidEcpm));
      interstitialAd.setBidEcpm(bidEcpm, lossBidEcpm);
    } else {
      //模拟竞价失败
      @AdExposureFailureCode int failureCod = random.nextInt(5);
      AdExposureFailedReason reason = new AdExposureFailedReason();
      if (failureCod == AdExposureFailureCode.BID_FAILED) {
        mIsBidFailed = true;
        ToastUtil.showToast(context, "竞价失败");
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
        reason.setAdUserName("新插屏 广告主名称-test");
        reason.setAdTitle("新插屏 广告标题-test");
        reason.setAdRequestId("新插屏 请求ID-test");
        reason.setAdnMaterialType(random.nextInt(7) + 1);
        reason.setAdnMaterialUrl("新插屏 广告素材url-test");
      }
      interstitialAd.reportAdExposureFailed(failureCod, reason);
    }
  }

  void testBidFailedClickAndShowReport(
      KsInterstitialAd ksInterstitialAd, AdActionType actionType) {
    AdExposureFailedReason reason = new AdExposureFailedReason();
    reason.setAdUserName("新插屏 广告主名称-test");
    reason.setAdTitle("新插屏 广告标题-test");
    reason.setAdRequestId("新插屏 请求ID-test");
    if (actionType == AdActionType.SHOW) {
      reason.setIsShow(AdShowAction.SHOW);
    } else if (actionType == AdActionType.CLICK) {
      reason.setIsClick(AdClickAction.CLICK);
    }
    ksInterstitialAd.reportAdExposureFailed(AdExposureFailureCode.BID_FAILED, reason);
  }

  public enum AdActionType {
    UNKNOWN,
    CLICK,
    SHOW
  }
  /**********DevelopCode End***********/

  @Override
  public void onClick(View v) {
    if (v == mBackBtn) {
      finish();
    }
  }

  private static class MyInterLoadListener implements KsLoadManager.InterstitialAdListener{
    private WeakReference<TestNewInterstitialAdActivity> mHost;

    public MyInterLoadListener(TestNewInterstitialAdActivity activity) {
      mHost = new WeakReference<>(activity);
    }

    private TestNewInterstitialAdActivity obtain() {
      if (mHost == null) {
        return null;
      }

      return mHost.get();
    }

    public void destroy() {
      mHost = null;
    }

    @Override
    public void onError(int code, String msg) {
      TestNewInterstitialAdActivity activity = obtain();
      if (activity == null) {
        return;
      }

      ToastUtil.showToast(activity, "插屏广告请求失败" + code + msg);
      LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "loadInterstitialAd_onError");
      activity.isAdRequesting = false;
    }

    @Override
    public void onRequestResult(int adNumber) {
      TestNewInterstitialAdActivity activity = obtain();
      if (activity == null) {
        return;
      }
      LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onRequestResult");
      ToastUtil.showToast(activity, "插屏广告请求填充个数 " + adNumber);
    }


    @Override
    public void onInterstitialAdLoad(@Nullable List<KsInterstitialAd> adList) {
      TestNewInterstitialAdActivity activity = obtain();
      if (activity == null) {
        return;
      }
      activity.isAdRequesting = false;
      if (adList != null && adList.size() > 0) {
        activity.mKsInterstitialAd = adList.get(0);
        ToastUtil.showToast(activity, "插屏广告请求成功");
        LogUtils.recodeCallback(LogUtils.SCENE_INSERT_PRE, "onInterstitialAdLoad");
        KsVideoPlayConfig videoPlayConfig = new KsVideoPlayConfig.Builder()
            .videoSoundEnable(!activity.mVideoSoundSwitch.isChecked())
            .showLandscape(activity.mChangeOrientation.isChecked())
            .build();
        activity.showInterstitialAd(videoPlayConfig);
      }
    }
  }
}
