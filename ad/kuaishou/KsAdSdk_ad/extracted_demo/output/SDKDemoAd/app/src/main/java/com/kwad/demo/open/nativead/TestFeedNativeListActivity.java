package com.kwad.demo.open.nativead;

import static com.kwad.demo.open.feed.FeedHomeActivity.POS_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.kwad.demo.R;
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.utils.LogUtils;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.demo.open.view.LoadMoreListView;
import com.kwad.sdk.api.KsAdVideoPlayConfig;
import com.kwad.sdk.api.KsAppDownloadListener;
import com.kwad.sdk.api.KsImage;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsNativeAd;
import com.kwad.sdk.api.KsApkDownloadListener;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.model.AdClickAction;
import com.kwad.sdk.api.model.AdExposureFailedReason;
import com.kwad.sdk.api.model.AdExposureFailureCode;
import com.kwad.sdk.api.model.AdShowAction;
import com.kwad.sdk.api.model.AdSourceLogoType;
import com.kwad.sdk.api.model.InteractionType;
import com.kwad.sdk.api.model.KsNativeConvertType;
import com.kwad.sdk.api.model.MaterialType;
import com.kwad.sdk.api.model.NativeAdExtraData;

public class TestFeedNativeListActivity extends Activity {

  private Context mContext;
  private List<KsNativeAd> mKsNativeAdList;
  private FeedListAdapter mFeedListAdapter;
  private Handler mHandler = new Handler(Looper.getMainLooper());
  private LoadMoreListView mListView;
  private long mPosId;

  /**********DevelopCode Start***********/
  //模拟展示的是否是快手广告
  private static boolean mIsBidFailed = false;
  /**********DevelopCode End***********/

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mContext = this;
    setContentView(R.layout.activity_test_feed_native_list);
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POSID_ENTRY_TYPE4.posId);

    initView();
  }

  private void initView() {
    mListView = findViewById(R.id.feed_list);
    mKsNativeAdList = new ArrayList<>();
    mFeedListAdapter = new FeedListAdapter(this, mKsNativeAdList);
    mListView.setAdapter(mFeedListAdapter);
    mListView.setLoadMoreListener(new LoadMoreListView.ILoadMoreListener() {
      @Override
      public void onLoadMore() {
        requestAd(mPosId);
      }
    });
    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        requestAd(mPosId);
      }
    }, 500);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mHandler.removeCallbacksAndMessages(null);
  }

  /**
   * 请求Feed默认模板广告数据
   */
  private void requestAd(long posId) {
    NativeAdExtraData nativeAdExtraData = new NativeAdExtraData();
    nativeAdExtraData.setShowLiveStatus(1);
    nativeAdExtraData.setShowLiveStyle(1);
    nativeAdExtraData.setEnableRotate(true);
    // 此为测试posId，请联系快手平台申请正式posId,
    KsScene scene = KSSdkInitUtil.createKSSceneBuilder(posId)
        .setNativeAdExtraData(nativeAdExtraData)
        .adNum(3)
        .build();
    // 支持返回多条广告，默认1条，最多5条，参数范围1-5
    KSSdkInitUtil.getLoadManager().loadNativeAd(scene, new KsLoadManager.NativeAdListener() {
      @Override
      public void onError(int code, String msg) {
        if (mListView != null) {
          mListView.setLoadingError();
        }
        LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "loadNativeAd_onError");
        ToastUtil.showToast(mContext, "广告数据请求失败" + code + msg);
      }

      @Override
      public void onNativeAdLoad(@Nullable List<KsNativeAd> adList) {
        if (mListView != null) {
          mListView.setLoadingFinish();
        }
        if (adList == null || adList.isEmpty()) {
          ToastUtil.showToast(mContext, "广告数据为空");
          return;
        }
        int loadCount = 15;// 模拟每次展示的Item刷新个数
        for (int i = 0; i < loadCount; i++) {
          mKsNativeAdList.add(null);
        }
        int totalCount = mKsNativeAdList.size();
        for (KsNativeAd ksNativeAd : adList) {
          if (ksNativeAd == null) {
            continue;
          }

          /**********DevelopCode Start***********/
          // 模拟竞价
          mIsBidFailed = TestNativeAdActivity.testBiddingReport(mContext, ksNativeAd);
          /**********DevelopCode End***********/

          int random = (int) (Math.random() * loadCount) + totalCount - loadCount;
          mKsNativeAdList.add(random, ksNativeAd);
        }
        LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onNativeAdLoad");
        mFeedListAdapter.notifyDataSetChanged();
      }
    });
  }

  private static class FeedListAdapter extends BaseAdapter {
    private Map<AdBaseViewHolder, KsAppDownloadListener> mAppDownloadListenerMap =
        new WeakHashMap<>();
    private List<KsNativeAd> mKsNativeAdList;
    private Context mContext;
    private Activity mActivity;

    FeedListAdapter(Activity context, List<KsNativeAd> feedList) {
      this.mActivity = context;
      this.mContext = context.getApplicationContext();
      this.mKsNativeAdList = feedList;
    }

    @Override
    public int getCount() {
      return mKsNativeAdList.size();
    }

    @Override
    public KsNativeAd getItem(int position) {
      return mKsNativeAdList.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public int getItemViewType(int position) {
      KsNativeAd ksNativeAd = getItem(position);
      if (ksNativeAd == null) {
        return ItemViewType.ITEM_VIEW_TYPE_NORMAL;
      } else {
        switch (ksNativeAd.getMaterialType()) {
          case MaterialType.VIDEO:
          case MaterialType.ORIGIN_LIVE:
            return ItemViewType.ITEM_VIEW_TYPE_VIDEO;
          case MaterialType.SINGLE_IMG:
            return ItemViewType.ITEM_VIEW_TYPE_SINGLE_IMG;
          case MaterialType.GROUP_IMG:
            return ItemViewType.ITEM_VIEW_TYPE_GROUP_IMG;
          case MaterialType.UNKNOWN:
          default:
            return ItemViewType.ITEM_VIEW_TYPE_NORMAL;
        }
      }
    }

    @Override
    public int getViewTypeCount() {
      return 4;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      KsNativeAd ksNativeAd = getItem(position);
      switch (getItemViewType(position)) {
        case ItemViewType.ITEM_VIEW_TYPE_VIDEO:
          return getVideoItemView(convertView, parent, ksNativeAd);
        case ItemViewType.ITEM_VIEW_TYPE_SINGLE_IMG:
          return getSingleImageItemView(convertView, parent, ksNativeAd);
        case ItemViewType.ITEM_VIEW_TYPE_GROUP_IMG:
          return getGroupImageItemView(convertView, parent, ksNativeAd);
        case ItemViewType.ITEM_VIEW_TYPE_NORMAL:
        default:
          return getNormalItemView(convertView, parent, position);
      }
    }

    private View getVideoItemView(View convertView, ViewGroup parent, KsNativeAd ksNativeAd) {
      AdVideoViewHolder videoViewHolder;
      if (convertView == null) {
        convertView =
            LayoutInflater.from(mContext).inflate(R.layout.native_item_video, parent, false);
        videoViewHolder = new AdVideoViewHolder(convertView);
        convertView.setTag(videoViewHolder);
      } else {
        videoViewHolder = (AdVideoViewHolder) convertView.getTag();
      }


      /** SDK默认渲染的视频view **/
      ksNativeAd.setVideoPlayListener(new KsNativeAd.VideoPlayListener() {
        @Override
        public void onVideoPlayReady() {
          ToastUtil.showToast(mContext.getApplicationContext(), "onVideoPlayReady");
          LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayReady");
        }

        @Override
        public void onVideoPlayStart() {
          ToastUtil.showToast(mContext.getApplicationContext(), "onVideoPlayStart");
          LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayStart");
        }

        @Override
        public void onVideoPlayComplete() {
          ToastUtil.showToast(mContext.getApplicationContext(), "onVideoPlayComplete");
          LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayComplete");
        }

        @Override
        public void onVideoPlayError(int what, int extra) {
          ToastUtil.showToast(mContext.getApplicationContext(), "onVideoPlayError");
          LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayError");
        }

        @Override
        public void onVideoPlayPause() {
          ToastUtil.showToast(mContext.getApplicationContext(), "onVideoPlayPause");
          LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayPause");
        }

        @Override
        public void onVideoPlayResume() {
          ToastUtil.showToast(mContext.getApplicationContext(), "onVideoPlayResume");
          LogUtils.recodeCallback(LogUtils.SCENE_NATIVE_PRE, "onVideoPlayResume");
        }
      });

      // SDK默认渲染的视频view
      KsAdVideoPlayConfig videoPlayConfig = new KsAdVideoPlayConfig.Builder()
          .build();
      View videoView = ksNativeAd.getVideoView(mContext, videoPlayConfig);
      if (videoView != null && videoView.getParent() == null) {
        videoViewHolder.mAdVideoContainer.removeAllViews();
        videoViewHolder.mAdVideoContainer.addView(videoView);
      }
      // 获取扭摇view
      if (ksNativeAd.enableRotate()) {
        View rotateView = ksNativeAd.getRotateView(mContext);
        if (rotateView != null && rotateView.getParent() == null) {
          FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
          layoutParams.gravity = Gravity.CENTER;
          videoViewHolder.mAdVideoContainer.addView(rotateView, layoutParams);
        }
      }

      // 设置广告数据
      bindCommonData((ViewGroup) convertView, videoViewHolder, ksNativeAd);
      return convertView;
    }

    // 可支持自渲染视频view
    protected View getVideoItemView2(ViewGroup parent, KsNativeAd ksNativeAd) {
      View convertView =
          LayoutInflater.from(mContext).inflate(R.layout.native_item_video, parent, false);
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
//      // 2.请在视频播放结束时调用此方法（每次播放到最后1s，暂停恢复除外）
      ksNativeAd.reportAdVideoPlayEnd();
      /** 自渲染视频view end **/
      return convertView;
    }

    private View getSingleImageItemView(View convertView, ViewGroup parent,
        KsNativeAd ksNativeAd) {

      AdSingleImageViewHolder viewHolder;
      if (convertView == null) {
        convertView =
            LayoutInflater.from(mContext).inflate(R.layout.native_item_single_image, parent, false);
        viewHolder = new AdSingleImageViewHolder(convertView);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (AdSingleImageViewHolder) convertView.getTag();
      }

      // 设置广告数据
      bindCommonData((ViewGroup) convertView, viewHolder, ksNativeAd);

      // 获取图片资源
      if (ksNativeAd.getImageList() != null && !ksNativeAd.getImageList().isEmpty()) {
        KsImage image = ksNativeAd.getImageList().get(0);
        if (image != null && image.isValid()) {
          Glide.with(mContext).load(image.getImageUrl()).into(viewHolder.mAdImage);
        }
      }
      return convertView;
    }

    private View getGroupImageItemView(View convertView, ViewGroup parent, KsNativeAd ksNativeAd) {
      AdGroupImageViewHolder viewHolder;
      if (convertView == null) {
        convertView =
            LayoutInflater.from(mContext).inflate(R.layout.native_item_group_image, parent, false);
        viewHolder = new AdGroupImageViewHolder(convertView);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (AdGroupImageViewHolder) convertView.getTag();
      }

      // 设置广告数据
      bindCommonData((ViewGroup) convertView, viewHolder, ksNativeAd);

      // 获取图片资源
      List<KsImage> ksImageList = ksNativeAd.getImageList();
      if (ksImageList != null && !ksImageList.isEmpty()) {
        for (int i = 0; i < ksImageList.size(); i++) {
          KsImage image = ksNativeAd.getImageList().get(i);
          if (image != null && image.isValid()) {
            if (i == 0) {
              Glide.with(mContext).load(image.getImageUrl()).into(viewHolder.mAdImageLeft);
            } else if (i == 1) {
              Glide.with(mContext).load(image.getImageUrl()).into(viewHolder.mAdImageMid);
            } else if (i == 2) {
              Glide.with(mContext).load(image.getImageUrl()).into(viewHolder.mAdImageRight);
            }
          }
        }
      }
      return convertView;
    }

    @SuppressLint("DefaultLocale")
    private View getNormalItemView(View convertView, ViewGroup parent, int position) {
      NormalViewHolder viewHolder;
      if (convertView == null) {
        convertView =
            LayoutInflater.from(mContext).inflate(R.layout.native_item_normal, parent, false);
        viewHolder = new NormalViewHolder(convertView);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (NormalViewHolder) convertView.getTag();
      }
      viewHolder.textView.setText(String.format("ListView item %d", position));
      return convertView;
    }

    private void bindCommonData(final ViewGroup convertView, AdBaseViewHolder adBaseViewHolder,
        final KsNativeAd ad) {
      // 点击转换view的集合，传入的view点击时会触发转换操作：app下载， 打开h5页面
      Map<View,Integer> clickViewMap= new HashMap<>();
      clickViewMap.put(adBaseViewHolder.mAdContainer, KsNativeConvertType.CONVERT_SLIDE);
      clickViewMap.put(adBaseViewHolder.mAdConvertBtn, KsNativeConvertType.CONVERT);
      clickViewMap.put(adBaseViewHolder.mAdIcon, KsNativeConvertType.CONVERT_CLICK);
      clickViewMap.put(adBaseViewHolder.mAdName, KsNativeConvertType.CONVERT_CLICK);
      clickViewMap.put(adBaseViewHolder.mAdDes, KsNativeConvertType.SHOW_DOWNLOAD_TIPS_DIALOG);
      clickViewMap.put(adBaseViewHolder.mAdDesc, KsNativeConvertType.SHOW_DOWNLOAD_TIPS_DIALOG);

      if (adBaseViewHolder instanceof AdSingleImageViewHolder) {
        clickViewMap.put(((AdSingleImageViewHolder) adBaseViewHolder).mAdImage, KsNativeConvertType.CONVERT);
      }
      //视频需要支持点击请传入视频的view
      if (adBaseViewHolder instanceof AdVideoViewHolder) {
        clickViewMap.put(((AdVideoViewHolder) adBaseViewHolder).mAdVideoContainer.getChildAt(0),
            KsNativeConvertType.CONVERT_CLICK);
      }

     // 如果是自定义弹窗，请使用下面的配置
      // 注册View的点击，点击后触发转化
      ad.registerViewForInteraction(mActivity, convertView, clickViewMap,
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
      TestNativeAdActivity.logAdForTest(ad);
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
        Glide.with(mContext).load(ad.getAdSourceLogoUrl(grayMode ? AdSourceLogoType.GREY : AdSourceLogoType.NORMAL)).into(adBaseViewHolder.mAdLogoIcon);
        adBaseViewHolder.mAdSourceDesc.setTextColor(grayMode ? 0xFF9C9C9C : 0x99FFFFFF);
        adBaseViewHolder.mAdSourceDesc.setText(adSource);
      }
    }

    private void bindDownloadListener(final AdBaseViewHolder adBaseViewHolder,
        final KsNativeAd ad) {
      KsApkDownloadListener ksAppDownloadListener = new KsApkDownloadListener() {

        @Override
        public void onIdle() {
          if (isInvalidCallBack()) {
            return;
          }
          if (TextUtils.isEmpty(ad.getActionDescription())) {
            adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
          } else {
            adBaseViewHolder.mAdConvertBtn.setText("立即下载");
          }
        }

        @Override
        public void onDownloadStarted() {
          adBaseViewHolder.mAdConvertBtn.setText("开始下载");
        }

        @Override
        public void onProgressUpdate(int progress) {
          if (isInvalidCallBack()) {
            return;
          }
          if (ad.getMaterialType() == MaterialType.ORIGIN_LIVE) {
            adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
          } else {
            adBaseViewHolder.mAdConvertBtn.setText(String.format("%s/100", progress));
          }
        }

        @Override
        public void onPaused(int progress) {
          if (isInvalidCallBack()) {
            return;
          }
          if (ad.getMaterialType() == MaterialType.ORIGIN_LIVE) {
            adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
          } else {
            adBaseViewHolder.mAdConvertBtn.setText("恢复下载");
          }
        }

        @Override
        public void onDownloadFinished() {
          if (isInvalidCallBack()) {
            return;
          }
          if (ad.getMaterialType() == MaterialType.ORIGIN_LIVE) {
            adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
          } else {
            adBaseViewHolder.mAdConvertBtn.setText("立即安装");
          }
        }

        @Override
        public void onDownloadFailed() {
          if (isInvalidCallBack()) {
            return;
          }
          if (TextUtils.isEmpty(ad.getActionDescription())) {
            adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
          } else {
            adBaseViewHolder.mAdConvertBtn.setText("立即下载");
          }
        }

        @Override
        public void onInstalled() {
          if (isInvalidCallBack()) {
            return;
          }
          if (ad.getMaterialType() == MaterialType.ORIGIN_LIVE) {
            adBaseViewHolder.mAdConvertBtn.setText(ad.getActionDescription());
          } else {
            adBaseViewHolder.mAdConvertBtn.setText("立即打开");
          }
        }

        private boolean isInvalidCallBack() {
          return mAppDownloadListenerMap.get(adBaseViewHolder) != this;
        }

      };
      // 由于ViewHolder复用的原因，一个ViewHolder可能对应多个downloadListener,
      // 所以保持ViewHolder最新对应的downloadListener的对应关系，
      // 通过isInvalidCallBack判断当前ViewHolder绑定的listener是不是自己.
      mAppDownloadListenerMap.put(adBaseViewHolder, ksAppDownloadListener);
      ad.setDownloadListener(ksAppDownloadListener);// 注册下载监听器
    }

    @IntDef({ItemViewType.ITEM_VIEW_TYPE_NORMAL, ItemViewType.ITEM_VIEW_TYPE_VIDEO,
        ItemViewType.ITEM_VIEW_TYPE_SINGLE_IMG, ItemViewType.ITEM_VIEW_TYPE_GROUP_IMG})
    @interface ItemViewType {
      int ITEM_VIEW_TYPE_NORMAL = 0;
      int ITEM_VIEW_TYPE_VIDEO = 1;
      int ITEM_VIEW_TYPE_SINGLE_IMG = 2;
      int ITEM_VIEW_TYPE_GROUP_IMG = 3;
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

    /**********DevelopCode Start***********/
    void testBidFailedClickAndShowReport(
        KsNativeAd ad, AdActionType actionType) {
      AdExposureFailedReason reason = new AdExposureFailedReason();
      reason.setAdUserName("信息流自渲染 广告主名称-test");
      reason.setAdTitle("信息流自渲染 广告标题-test");
      reason.setAdRequestId("信息流自渲染 请求ID-test");
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

}
