package com.kwad.demo.open.nativead;

import static com.kwad.demo.open.feed.FeedHomeActivity.POS_ID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.kwad.demo.R;
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.serverBid.BiddingDemoUtils;
import com.kwad.demo.open.utils.LogUtils;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.sdk.api.KsAdVideoPlayConfig;
import com.kwad.sdk.api.KsApkDownloadListener;
import com.kwad.sdk.api.KsImage;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsNativeAd;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.model.AdClickAction;
import com.kwad.sdk.api.model.AdExposureFailedReason;
import com.kwad.sdk.api.model.AdExposureFailureCode;
import com.kwad.sdk.api.model.AdShowAction;
import com.kwad.sdk.api.model.AdSourceLogoType;
import com.kwad.sdk.api.model.AdnName;
import com.kwad.sdk.api.model.AdnType;
import com.kwad.sdk.api.model.InteractionType;
import com.kwad.sdk.api.model.KsNativeConvertType;
import com.kwad.sdk.api.model.MaterialType;
import com.kwad.sdk.api.model.NativeAdExtraData;

public class TestNativeAdActivity extends Activity {
  protected Context mContext;
  private FrameLayout mNativeAdContainer;
  private View mBackBtn;
  private EditText posIdEditText;
  private long posIdEditTextNum;

  private boolean isShowInDialog;
  private Long mPosId;

  /**********DevelopCode Start***********/
  //模拟展示的是否是快手广告
  private boolean mIsBidFailed = false;
  /**********DevelopCode End***********/

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test_native_ad);
    mContext = this;
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POSID_NATIVE_INTERSTITIAL.posId);
    mNativeAdContainer = findViewById(R.id.native_ad_container);
    mBackBtn = findViewById(R.id.ksad_main_left_back_btn);
    if (mBackBtn != null) {
      mBackBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          finish();
        }
      });
    }
    posIdEditText = findViewById(R.id.native_ad_et);
    posIdEditText.addTextChangedListener(mTextWatcher);
  }

  public void requestNativeAdImage(View view) {
    requestAd(posIdEditTextNum == 0 ?
        TestPosId.POSID_NATIVE_IMAGE.posId : posIdEditTextNum);
  }

  public void requestNativeAdVideo(View view) {
    requestAd(posIdEditTextNum == 0 ?
        TestPosId.POSID_NATIVE_VIDEO.posId : posIdEditTextNum);
  }

  /**
   * 测试插屏,媒体自渲染+dialog
   */
  public void testDialogNative(View view) {
    isShowInDialog = true;
    requestAd(posIdEditTextNum == 0 ?
        TestPosId.POSID_NATIVE_INTERSTITIAL.posId : posIdEditTextNum);
  }

  /**
   * 测试信息流，媒体自渲染+ListView实现
   */
  public void testFeedNativeList(View view) {
    Intent intent = new Intent(TestNativeAdActivity.this, TestFeedNativeListActivity.class);
    intent.putExtra("posId", posIdEditTextNum == 0 ?
        TestPosId.POSID_FEED_TYPE_1.posId : posIdEditTextNum);
    startActivity(intent);
  }

  /**
   * 请求自渲染的广告数据
   */
  private void requestAd(long posId) {

    KsScene scene = KSSdkInitUtil.createKSSceneBuilder(posId) // 此为测试posId，请联系快手平台申请正式posId
        .adNum(1)  // 支持返回多条广告，默认1条，最多5条，参数范围1-5
        .setNativeAdExtraData(new NativeAdExtraData()
            .setShowLiveStatus(1)
            .setShowLiveStyle(1)
            .setEnableRotate(false))
        .build();
    if (!TextUtils.isEmpty(mBidResponseV1)) { // 设置在服务端竞价后的广告信息
      scene.setBidResponse(mBidResponseV1);
    } else if (!TextUtils.isEmpty(mBidResponseV2)) {
      scene.setBidResponseV2(mBidResponseV2);
    }
    KSSdkInitUtil.getLoadManager().loadNativeAd(scene, new KsLoadManager.NativeAdListener() {
      @Override
      public void onError(int code, String msg) {
        ToastUtil.showToast(mContext, "广告数据请求失败" + code + msg);
        LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "loadNativeAd_onError");
      }

      @Override
      public void onNativeAdLoad(@Nullable List<KsNativeAd> adList) {
        if (adList == null || adList.isEmpty()) {
          ToastUtil.showToast(mContext, "广告数据为空");
          return;
        }
        LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onNativeAdLoad");
        /**********DevelopCode Start***********/
        // 模拟竞价
        mIsBidFailed = testBiddingReport(mContext, adList.get(0));
        /**********DevelopCode End***********/
        showAd(adList.get(0));
      }
    });
  }

  private String mBidResponseV1;
  private String mBidResponseV2;
  public void fetchBidResponseV1(View view) {
    // 根据需要传入场景参数，需传入有效posId
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(mPosId)
        .build();
    String token = KSSdkInitUtil.getLoadManager().getBidRequestToken(ksScene);
    BiddingDemoUtils.fetchBidResponse(this, TestPosId.POSID_NATIVE_INTERSTITIAL.posId, token, false,
        new BiddingDemoUtils.FetchResponseCallback() {
          @Override
          public void onSuccess(String bidResponse) {
            mBidResponseV1 = bidResponse;
          }
        });
  }

  public void showAdV1(View view) {
    if (TextUtils.isEmpty(mBidResponseV1)) {
      ToastUtil.showToast(TestNativeAdActivity.this, "请先获取竞价信息");
      return;
    }
    isShowInDialog = true;
    requestAd(posIdEditTextNum == 0 ?
        TestPosId.POSID_NATIVE_INTERSTITIAL.posId : posIdEditTextNum);
  }

  public void fetchBidResponseV2(View view) {
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(0)
        .build(); // 根据需要传入场景参数，注意：创建KsScene时 posId 可传无效值，在adx服务端拉取快手竞价信息时必须传有效的 posId
    String token = KSSdkInitUtil.getLoadManager().getBidRequestTokenV2(ksScene);
    BiddingDemoUtils.fetchBidResponse(this, TestPosId.POSID_NATIVE_INTERSTITIAL.posId, token, true,
        new BiddingDemoUtils.FetchResponseCallback() {
          @Override
          public void onSuccess(String bidResponse) {
            mBidResponseV2 = bidResponse;
          }
        });
  }

  public void showAdV2(View view) {
    if (TextUtils.isEmpty(mBidResponseV2)) {
      ToastUtil.showToast(TestNativeAdActivity.this, "请先获取竞价信息");
      return;
    }
    isShowInDialog = true;
    requestAd(posIdEditTextNum == 0 ?
        TestPosId.POSID_NATIVE_INTERSTITIAL.posId : posIdEditTextNum);
  }

  /**********DevelopCode Start***********/
  static boolean testBiddingReport(Context context, KsNativeAd ksNativeAd) {
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE,
        "getECPM() =" + ksNativeAd.getECPM());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE,
        "getMaterialType() =" + ksNativeAd.getMaterialType());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE,
        "getInteractionType() =" + ksNativeAd.getInteractionType());
    Random random = new Random();
    boolean isBidFailed = false;
    if (random.nextBoolean()) {
      //模拟竞价成功
      int bidEcpm = random.nextInt(9999) + 1;
      int lossBidEcpm = random.nextInt(bidEcpm);
      LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE,
          "setBidEcpm() =" + bidEcpm);
      ksNativeAd.setBidEcpm(bidEcpm);
      LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE,
          "setBidEcpm() =" + (bidEcpm + "|" + lossBidEcpm));
      ksNativeAd.setBidEcpm(bidEcpm, lossBidEcpm);
    } else {
      //模拟竞价失败
      @AdExposureFailureCode int failureCod = random.nextInt(5);
      AdExposureFailedReason reason = new AdExposureFailedReason();
      if (failureCod == AdExposureFailureCode.BID_FAILED) {
        isBidFailed = true;
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
        reason.setAdUserName("自渲染 广告主名称-test");
        reason.setAdTitle("自渲染 广告标题-test");
        reason.setAdRequestId("自渲染 请求ID-test");
        reason.setAdnMaterialType(random.nextInt(7) + 1);
        reason.setAdnMaterialUrl("自渲染 广告素材url-test");
      }
      ksNativeAd.reportAdExposureFailed(failureCod, reason);
    }
    return isBidFailed;
  }

  void testBidFailedClickAndShowReport(
      KsNativeAd ad, AdActionType actionType) {
    AdExposureFailedReason reason = new AdExposureFailedReason();
    reason.setAdUserName("自渲染 广告主名称-test");
    reason.setAdTitle("自渲染 广告标题-test");
    reason.setAdRequestId("自渲染 请求ID-test");
    reason.setAdnMaterialType(new Random().nextInt(7) + 1);
    reason.setAdnMaterialUrl("自渲染 广告素材url-test");
    if (actionType == AdActionType.SHOW) {
      reason.setIsShow(AdShowAction.SHOW);
    } else if (actionType == AdActionType.CLICK) {
      reason.setIsClick(AdClickAction.CLICK);
    }
    ad.reportAdExposureFailed(AdExposureFailureCode.BID_FAILED, reason);
  }

  public enum AdActionType {
    UNKNOWN,
    CLICK,
    SHOW
  }
  /**********DevelopCode End***********/

  /**
   * 加载自渲染View
   */
  private void showAd(KsNativeAd ksNativeAd) {
    mNativeAdContainer.removeAllViews();
    View adView;
    // 判断广告素材类型
    switch (ksNativeAd.getMaterialType()) {
      case MaterialType.VIDEO:
      case MaterialType.ORIGIN_LIVE:
        // 视频素材，渲染自定义的视频广告
        adView = getVideoItemView(mNativeAdContainer, ksNativeAd);
        break;
      case MaterialType.SINGLE_IMG:
        // 单图素材，渲染自定义的单图广告
        adView = getSingleImageItemView(mNativeAdContainer, ksNativeAd);
        break;
      case MaterialType.GROUP_IMG:
        // 组图素材，渲染自定义的组图广告
        adView = getGroupImageItemView(mNativeAdContainer, ksNativeAd);
        break;
      case MaterialType.UNKNOWN:
      default:
        adView = getNormalItemView(mNativeAdContainer);
    }
    if (adView != null && adView.getParent() == null) {
      if (isShowInDialog){
        TestNativeInterstitialDnialog dialog = new TestNativeInterstitialDnialog(this, adView);
        dialog.show();
      } else {
        mNativeAdContainer.addView(adView);
      }
    }
  }

  /**
   * 使用SDK渲染的播放控件, 直播 or 普通视频
   */
  protected View getVideoItemView(ViewGroup parent, KsNativeAd ksNativeAd) {
    View convertView =
        LayoutInflater.from(this).inflate(R.layout.native_item_video, parent, false);
    AdVideoViewHolder videoViewHolder = new AdVideoViewHolder(convertView);

    ksNativeAd.setVideoPlayListener(new KsNativeAd.VideoPlayListener() {
      @Override
      public void onVideoPlayReady() {
        ToastUtil.showToast(getApplicationContext(), "onVideoPlayReady");
        LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayReady");
      }

      @Override
      public void onVideoPlayStart() {
        ToastUtil.showToast(getApplicationContext(), "onVideoPlayStart");
        LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayStart");
      }

      @Override
      public void onVideoPlayComplete() {
        ToastUtil.showToast(getApplicationContext(), "onVideoPlayComplete");
        LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayComplete");
      }

      @Override
      public void onVideoPlayError(int what, int extra) {
        ToastUtil.showToast(getApplicationContext(), "onVideoPlayError");
        LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayError");
      }

      @Override
      public void onVideoPlayPause() {
        ToastUtil.showToast(getApplicationContext(), "onVideoPlayPause");
        LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayPause");
      }

      @Override
      public void onVideoPlayResume() {
        ToastUtil.showToast(getApplicationContext(), "onVideoPlayResume");
        LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayResume");
      }
    });

    // SDK默认渲染的视频view
    KsAdVideoPlayConfig videoPlayConfig = new KsAdVideoPlayConfig.Builder()
        .dataFlowAutoStart(true) // 流量下自动播放
        .videoAutoPlayType(
            KsAdVideoPlayConfig.VideoAutoPlayType.AUTO_PLAY) // 设置在有wifi
        // 时视频自动播放，当与dataFlowAutoStart()同时设置时，以最后一个传入的值为准
        .build();
    View videoView = ksNativeAd.getVideoView(mContext, videoPlayConfig);
    if (videoView != null && videoView.getParent() == null) {
      videoViewHolder.mAdVideoContainer.removeAllViews();
      videoViewHolder.mAdVideoContainer.addView(videoView);
    }

    // 获取扭摇view
    if (ksNativeAd.enableRotate()) {
      View rotateView = ksNativeAd.getRotateView(mContext);
      if (rotateView != null) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        videoViewHolder.mAdVideoContainer.addView(rotateView, layoutParams);
      }
    }
    // 设置广告数据
    bindCommonData((ViewGroup) convertView, videoViewHolder, ksNativeAd);

    return convertView;
  }

  /**
   * 使用媒体自己渲染的播放控件
   */
  protected View getVideoItemView2(ViewGroup parent, KsNativeAd ksNativeAd) {
    View convertView =
        LayoutInflater.from(this).inflate(R.layout.native_item_video, parent, false);
    AdVideoViewHolder viewHolder = new AdVideoViewHolder(convertView);

    // 设置广告数据
    bindCommonData((ViewGroup) convertView, viewHolder, ksNativeAd);

    /** 媒体也可以自渲染视频view start **/
    // 获取视频地址
    String videoUrl = ksNativeAd.getVideoUrl();
    // 获取视频时长
    int videoDuration = ksNativeAd.getVideoDuration();
    // 获取视频封面图片
    KsImage ksImage = ksNativeAd.getVideoCoverImage();

    // 特别注意，视频播放需要客户自渲染，所以需要客户在合适时机掉如下方法进行打点，用于统计视频观看时长
    // 1.请在视频播放开始时调用此方法（每次从0秒开始播放，暂停恢复除外）
    ksNativeAd.reportAdVideoPlayStart();
//    // 2.请在视频播放结束时调用此方法（每次播放到最后1s，暂停恢复除外）
    ksNativeAd.reportAdVideoPlayEnd();
    /** 自渲染视频view end **/
    return convertView;
  }

  protected View getSingleImageItemView(ViewGroup parent, KsNativeAd ksNativeAd) {
    View convertView =
        LayoutInflater.from(this).inflate(R.layout.native_item_single_image, parent, false);
    AdSingleImageViewHolder viewHolder = new AdSingleImageViewHolder(convertView);
    bindCommonData((ViewGroup) convertView, viewHolder, ksNativeAd);

    // 获取图片资源
    if (ksNativeAd.getImageList() != null && !ksNativeAd.getImageList().isEmpty()) {
      KsImage image = ksNativeAd.getImageList().get(0);
      if (image != null && image.isValid()) {
        Glide.with(this).load(image.getImageUrl()).into(viewHolder.mAdImage);
      }
    }
    return convertView;
  }

  protected View getGroupImageItemView(ViewGroup parent, KsNativeAd ksNativeAd) {
    View convertView =
        LayoutInflater.from(this).inflate(R.layout.native_item_group_image, parent, false);
    AdGroupImageViewHolder viewHolder = new AdGroupImageViewHolder(convertView);
    bindCommonData((ViewGroup) convertView, viewHolder, ksNativeAd);

    // 获取图片资源
    List<KsImage> ksImageList = ksNativeAd.getImageList();
    if (ksImageList != null && !ksImageList.isEmpty()) {
      for (int i = 0; i < ksImageList.size(); i++) {
        KsImage image = ksNativeAd.getImageList().get(i);
        if (image != null && image.isValid()) {
          if (i == 0) {
            Glide.with(this).load(image.getImageUrl()).into(viewHolder.mAdImageLeft);
          } else if (i == 1) {
            Glide.with(this).load(image.getImageUrl()).into(viewHolder.mAdImageMid);
          } else if (i == 2) {
            Glide.with(this).load(image.getImageUrl()).into(viewHolder.mAdImageRight);
          }
        }
      }
    }
    return convertView;
  }

  @SuppressLint("DefaultLocale")
  protected View getNormalItemView(ViewGroup parent) {
    View convertView =
        LayoutInflater.from(this).inflate(R.layout.native_item_normal, parent, false);
    NormalViewHolder normalViewHolder = new NormalViewHolder(convertView);
    normalViewHolder.textView.setText("没有广告");
    return convertView;
  }

  private void bindCommonData(ViewGroup convertView, AdBaseViewHolder adBaseViewHolder,
      final KsNativeAd ad) {
    // 点击转换view的集合，传入的view点击时会触发转换操作：app下载， 打开h5页面
    Map<View, Integer> clickViewMap = new HashMap<>();
    clickViewMap.put(adBaseViewHolder.mAdContainer, KsNativeConvertType.CONVERT);
    clickViewMap.put(adBaseViewHolder.mAdConvertBtn, KsNativeConvertType.CONVERT);
    clickViewMap.put(adBaseViewHolder.mAdIcon, KsNativeConvertType.CONVERT);
    clickViewMap.put(adBaseViewHolder.mAdName, KsNativeConvertType.SHOW_DOWNLOAD_TIPS_DIALOG);
    clickViewMap.put(adBaseViewHolder.mAdDes, KsNativeConvertType.SHOW_DOWNLOAD_TIPS_DIALOG);
    clickViewMap.put(adBaseViewHolder.mAdDesc, KsNativeConvertType.SHOW_DOWNLOAD_TIPS_DIALOG);

    if (adBaseViewHolder instanceof AdSingleImageViewHolder) {
      clickViewMap.put(((AdSingleImageViewHolder) adBaseViewHolder).mAdImage,
          KsNativeConvertType.CONVERT);
    }
    //视频需要支持点击请传入视频的view 转化行为由sdk控制
    if (adBaseViewHolder instanceof AdVideoViewHolder) {
      clickViewMap.put(((AdVideoViewHolder) adBaseViewHolder).mAdVideoContainer.getChildAt(0),
          KsNativeConvertType.CONVERT_SLIDE);
    }

    // 如果是自定义弹窗，请使用下面的配置
    // 注册View的点击，点击后触发转化
    ad.registerViewForInteraction(this, convertView, clickViewMap,
        new KsNativeAd.AdInteractionListener() {
          @Override
          public void onAdClicked(View view, KsNativeAd ad) {
            if (ad != null) {
              ToastUtil.showToast(mContext, "广告" + ad.getAppName() + "被点击");
              LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onAdClicked");
              /**********DevelopCode Start***********/
              if (mIsBidFailed) {
                //模拟竞价失败，展示其他广告时点击上报
                testBidFailedClickAndShowReport(ad, AdActionType.CLICK);
              }
              /**********DevelopCode End***********/
            }
          }

          @Override
          public void onAdShow(KsNativeAd ad) {
            if (ad != null) {
              ToastUtil.showToast(mContext, "广告" + ad.getAppName() + "展示");
              LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onAdShow");
              /**********DevelopCode Start***********/
              if (mIsBidFailed) {
                //模拟竞价失败，展示其他广告时曝光上报
                testBidFailedClickAndShowReport(ad, AdActionType.SHOW);
              }
              /**********DevelopCode End***********/
            }
          }

          /*
           *  @return  返回为true, 则只会给媒体弹出回调，SDK的默认弹窗逻辑不会执行
           * @return  返回为true 返回为 false, 则使用SDK默认的合规弹窗。
           * 弹出弹窗dialog后， 用户确认下载，则媒体需要回调 OnClickListener.onClick(dialog, DialogInterface
           * .BUTTON_POSITIVE)
           * 弹出弹窗dialog后， 用户点击取消，则媒体需要回调 OnClickListener.onClick(dialog, DialogInterface
           * .BUTTON_NEGATIVE)
           * */
          @Override
          public boolean handleDownloadDialog(DialogInterface.OnClickListener clickListener) {
            LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "handleDownloadDialog");
            return false;
          }

          @Override
          public void onDownloadTipsDialogShow() {
            ToastUtil.showToast(mContext, "广告展示下载合规弹窗");
            LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onDownloadTipsDialogShow");
          }

          @Override
          public void onDownloadTipsDialogDismiss() {
            ToastUtil.showToast(mContext, "广告关闭下载合规弹窗");
            LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onDownloadTipsDialogDismiss");
          }
        });

    /**********DevelopCode Start***********/
    logAdForTest(ad);
    /**********DevelopCode End***********/

    // 其他数据
    Log.d("AppInfo", "应用名字 = " + ad.getAppName());
    Log.d("AppInfo", "应用包名 = " + ad.getAppPackageName());
    Log.d("AppInfo", "应用版本 = " + ad.getAppVersion());
    Log.d("AppInfo", "开发者 = " + ad.getCorporationName());
    Log.d("AppInfo", "包大小 = " + ad.getAppPackageSize());
    Log.d("AppInfo", "隐私条款链接 = " + ad.getAppPrivacyUrl());
    Log.d("AppInfo", "权限信息 = " + ad.getPermissionInfo());
    Log.d("AppInfo", "权限信息链接 = " + ad.getPermissionInfoUrl());
    Log.d("AppInfo", "功能介绍 = " + ad.getIntroductionInfo());
    Log.d("AppInfo", "功能介绍链接 = " + ad.getIntroductionInfoUrl());
    // 获取app的评分，取值范围0~5.0
    Log.d("AppInfo", "应用评分 = " + ad.getAppScore());
    // 获取app下载次数文案，例如：800W此下载，自行渲染。
    Log.d("AppInfo", "app下载次数文案 = " + ad.getAppDownloadCountDes());

    // 广告描述
    adBaseViewHolder.mAdDes.setText(ad.getAdDescription());
    String adIconUrl = ad.getAppIconUrl();
    // 广告icon
    if (!TextUtils.isEmpty(adIconUrl)) {
      Glide.with(mContext).load(adIconUrl).into(adBaseViewHolder.mAdIcon);
      adBaseViewHolder.mAdIcon.setVisibility(View.VISIBLE);
    } else {
      adBaseViewHolder.mAdIcon.setVisibility(View.GONE);
    }
    // 广告转化文案
    adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
    // 广告名称
    if (ad.getInteractionType() == InteractionType.DOWNLOAD) {
      adBaseViewHolder.mAdName.setText(ad.getAppName());
      // 下载类型的可以设置下载监听
      bindDownloadListener(adBaseViewHolder, ad);
    } else {
      adBaseViewHolder.mAdName.setText(ad.getProductName());
    }
    // 广告描述
    adBaseViewHolder.mAdDesc.setText(ad.getAdDescription());

    // 不喜欢
    adBaseViewHolder.mDislikeBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        ToastUtil.showToast(mContext, "广告" + ad.getAppName() + "不喜欢点击");
      }
    });
    // 广告来源
    String adSource = ad.getAdSource();
    boolean grayMode = true; // 开发者可根据实际需要调整
    if (TextUtils.isEmpty(adSource)) {
      adBaseViewHolder.mAdSourceDesc.setVisibility(View.GONE);
      adBaseViewHolder.mAdSourceDesc.setText("");
      adBaseViewHolder.mAdLogoIcon.setVisibility(View.GONE);
    } else {
      Glide.with(mContext)
          .load(ad.getAdSourceLogoUrl(grayMode ? AdSourceLogoType.GREY : AdSourceLogoType.NORMAL))
          .into(adBaseViewHolder.mAdLogoIcon);
      adBaseViewHolder.mAdSourceDesc.setTextColor(grayMode ? 0xFF9C9C9C : 0x99FFFFFF);
      adBaseViewHolder.mAdSourceDesc.setText(adSource);
    }
  }

  /**********DevelopCode Start***********/
  static void logAdForTest(KsNativeAd ad) {
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getAdDescription() =" + ad.getAdDescription());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getAdSource() =" + ad.getAdSource());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getImageList() =" + ad.getImageList());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getAdSourceLogoUrl() =" + ad.getAdSourceLogoUrl(AdSourceLogoType.NORMAL));
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getAppIconUrl() =" + ad.getAppIconUrl());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getAppName() =" + ad.getAppName());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getAppDownloadCountDes() =" + ad.getAppDownloadCountDes());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getAppScore() =" + ad.getAppScore());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getCorporationName() =" + ad.getCorporationName());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getPermissionInfo() =" + ad.getPermissionInfo());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getPermissionInfoUrl() =" + ad.getPermissionInfoUrl());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getIntroductionInfo() =" + ad.getIntroductionInfo());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getIntroductionInfoUrl() =" + ad.getIntroductionInfoUrl());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getAppPrivacyUrl() =" + ad.getAppPrivacyUrl());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getAppVersion() =" + ad.getAppVersion());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getAppPackageName() =" + ad.getAppPackageName());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getAppPackageSize() =" + ad.getAppPackageSize());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getVideoUrl() =" + ad.getVideoUrl());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getVideoCoverImage() =" + ad.getVideoCoverImage());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getVideoWidth() =" + ad.getVideoWidth());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getVideoHeight() =" + ad.getVideoHeight());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getVideoDuration() =" + ad.getVideoDuration());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getActionDescription() =" + ad.getActionDescription());
    LogUtils.recodeFuncLog(LogUtils.SCENE_NATIVE_PRE, "getProductName() =" + ad.getProductName());
  }
  /**********DevelopCode End***********/

  private TextWatcher mTextWatcher = new TextWatcher() {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
      posIdEditTextNum = 0;
      String tempPosIdEditTextStr = s.toString();
      if (!TextUtils.isEmpty(tempPosIdEditTextStr)) {
        try {
          posIdEditTextNum = Long.parseLong(tempPosIdEditTextStr);
        } catch (Exception e) {
        }
      }
    }
  };

  private void bindDownloadListener(final AdBaseViewHolder adBaseViewHolder, final KsNativeAd ad) {
    KsApkDownloadListener ksAppDownloadListener = new KsApkDownloadListener() {

      @Override
      public void onIdle() {
        adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
      }

      @Override
      public void onDownloadStarted() {
        if (ad.getMaterialType() == MaterialType.ORIGIN_LIVE) {
          adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
        } else {
          adBaseViewHolder.mAdConvertBtn.setText("开始下载");
        }
      }

      @Override
      public void onProgressUpdate(int progress) {
        if (ad.getMaterialType() == MaterialType.ORIGIN_LIVE) {
          adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
        } else {
          adBaseViewHolder.mAdConvertBtn.setText(String.format("%s/100", progress));
        }
      }

      @Override
      public void onPaused(int progress) {
        if (ad.getMaterialType() == MaterialType.ORIGIN_LIVE) {
          adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
        } else {
          adBaseViewHolder.mAdConvertBtn.setText("恢复下载");
        }
      }

      @Override
      public void onDownloadFinished() {
        if (ad.getMaterialType() == MaterialType.ORIGIN_LIVE) {
          adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
        } else {
          adBaseViewHolder.mAdConvertBtn.setText("立即安装");
        }
      }

      @Override
      public void onDownloadFailed() {
        adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
      }

      @Override
      public void onInstalled() {
        if (ad.getMaterialType() == MaterialType.ORIGIN_LIVE) {
          adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
        } else {
          adBaseViewHolder.mAdConvertBtn.setText("立即打开");
        }
      }

    };
    // 注册下载监听器
    ad.setDownloadListener(ksAppDownloadListener);
  }

  private static class NormalViewHolder {
    TextView textView;

    NormalViewHolder(View convertView) {
      this.textView = convertView.findViewById(R.id.tv);
    }
  }

  private static class AdSingleImageViewHolder extends AdBaseViewHolder {
    ImageView mAdImage;

    AdSingleImageViewHolder(View convertView) {
      super(convertView);
      mAdImage = convertView.findViewById(R.id.ad_image);
    }
  }

  private static class AdGroupImageViewHolder extends AdBaseViewHolder {
    ImageView mAdImageLeft;
    ImageView mAdImageMid;
    ImageView mAdImageRight;

    AdGroupImageViewHolder(View convertView) {
      super(convertView);
      mAdImageLeft = convertView.findViewById(R.id.ad_image_left);
      mAdImageMid = convertView.findViewById(R.id.ad_image_mid);
      mAdImageRight = convertView.findViewById(R.id.ad_image_right);
    }
  }

  private static class AdVideoViewHolder extends AdBaseViewHolder {
    FrameLayout mAdVideoContainer;

    AdVideoViewHolder(View convertView) {
      super(convertView);
      mAdVideoContainer = convertView.findViewById(R.id.video_container);
    }
  }

  private static class AdBaseViewHolder {
    TextView mAdDes;
    ImageView mAdIcon;
    TextView mAdName;
    TextView mAdDesc;
    TextView mAdConvertBtn;
    ImageView mDislikeBtn;
    ImageView mAdLogoIcon;
    TextView mAdSourceDesc;
    ViewGroup mAdContainer;

    AdBaseViewHolder(View convertView) {
      mAdDes = convertView.findViewById(R.id.ad_desc);
      mAdIcon = convertView.findViewById(R.id.app_icon);
      mAdName = convertView.findViewById(R.id.app_title);
      mAdDesc = convertView.findViewById(R.id.app_desc);
      mAdConvertBtn = convertView.findViewById(R.id.app_download_btn);
      mDislikeBtn = convertView.findViewById(R.id.ad_dislike);
      mAdLogoIcon = convertView.findViewById(R.id.ksad_logo_icon);
      mAdSourceDesc = convertView.findViewById(R.id.ksad_logo_text);
      mAdContainer = convertView.findViewById(R.id.ad_container);
    }
  }

}
