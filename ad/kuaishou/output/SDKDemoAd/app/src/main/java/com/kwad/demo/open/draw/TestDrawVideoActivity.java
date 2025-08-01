package com.kwad.demo.open.draw;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.kwad.demo.R;
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.draw.widget.OnViewPagerListener;
import com.kwad.demo.open.draw.widget.ViewPagerLayoutManager;
import com.kwad.demo.open.utils.LogUtils;
import com.kwad.demo.open.utils.TestSpUtil;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.sdk.api.KsDrawAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;

public class TestDrawVideoActivity extends Activity {
  private static final String TAG = "TestDrawVideoActivity";
  private Context mContext;
  private RecyclerView mRecyclerView;
  private ViewPagerLayoutManager mLayoutManager;
  private DrawRecyclerAdapter mRecyclerAdapter;
  private List<TestItem> mDrawList = new ArrayList<>();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    setContentView(R.layout.activity_test_draw);
    initView();
    initListener();
    requestAd(TestPosId.POSID_DRAW.posId);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mLayoutManager != null) {
      mLayoutManager.setOnViewPagerListener(null);
    }
  }

  // 1.请个Draw信息流广告，获取广告对象KsDrawAd
  private void requestAd(long posId) {
    KsScene scene = KSSdkInitUtil.createKSSceneBuilder(posId)
        .adNum(5)
        .build();
    KSSdkInitUtil.getLoadManager().loadDrawAd(scene, new KsLoadManager.DrawAdListener() {
      @Override
      public void onError(int code, String msg) {
        ToastUtil.showToast(mContext, "广告数据请求失败" + code + msg);
        LogUtils.recodeCallback(LogUtils.SCENE_DRAW_PRE, "loadDrawAd_onError");
      }

      @Override
      public void onDrawAdLoad(@Nullable List<KsDrawAd> adList) {
        if (adList == null || adList.isEmpty()) {
          ToastUtil.showToast(mContext, "广告数据为空");
          return;
        }
        List<TestItem.NormalVideo> normalVideoList = getTestVideo();
        for (TestItem.NormalVideo normalVideo : normalVideoList) {
          mDrawList.add(new TestItem(normalVideo, null));
        }
        for (KsDrawAd ksDrawAd : adList) {
          if (ksDrawAd == null) {
            continue;
          }
          int random = (int) (Math.random() * 100);
          int index = random % normalVideoList.size();
          if (index == 0) {
            index++;
          }
          mDrawList.add(index, new TestItem(null, ksDrawAd));
        }
        LogUtils.recodeCallback(LogUtils.SCENE_DRAW_PRE, "onDrawAdLoad");
        mRecyclerAdapter.notifyDataSetChanged();
      }
    });
  }

  private void initView() {
    mRecyclerView = findViewById(R.id.recycler_view);
    mLayoutManager = new ViewPagerLayoutManager(this, OrientationHelper.VERTICAL, false);
    mRecyclerAdapter = new DrawRecyclerAdapter(this, mDrawList);
    mRecyclerView.setLayoutManager(mLayoutManager);
    mRecyclerView.setAdapter(mRecyclerAdapter);
  }

  private void initListener() {
    mLayoutManager.setOnViewPagerListener(new OnViewPagerListener() {
      @Override
      public void onInitComplete() {
        Log.d(TAG, "初始化完成");
        if (!mDrawList.get(0).isAdVideoView()) {
          playVideo();
        }
      }

      @Override
      public void onPageRelease(boolean isNext, int position) {
        Log.d(TAG, "释放位置:" + position + " 下一页:" + isNext);
        int index = isNext ? 0 : 1;
        if (!mDrawList.get(position).isAdVideoView()) {
          releaseVideo(index);
        }
      }

      @Override
      public void onPageSelected(int position, boolean isBottom) {
        Log.d(TAG, "选中位置:" + position + "  是否是滑动到底部:" + isBottom);
        if (!mDrawList.get(position).isAdVideoView()) {
          playVideo();
        }
      }
    });
  }

  private void playVideo() {
    View itemView = mRecyclerView.getChildAt(0);
    if (itemView != null) {
      VideoView videoView = itemView.findViewById(R.id.video_view);
      final ImageView imgThumb = itemView.findViewById(R.id.video_thumb);
      if (!videoView.isPlaying()) {
        videoView.start();
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
          @Override
          public boolean onInfo(MediaPlayer mp, int what, int extra) {
            imgThumb.animate().alpha(0).setDuration(200).start();
            return false;
          }
        });
      } else {
        imgThumb.animate().alpha(0).setDuration(200).start();
      }
    }
  }

  private void releaseVideo(int index) {
    View itemView = mRecyclerView.getChildAt(index);
    if (itemView != null) {
      VideoView videoView = itemView.findViewById(R.id.video_view);
      if (videoView == null) {
        return;
      }
      ImageView imgThumb = itemView.findViewById(R.id.video_thumb);
      videoView.stopPlayback();
      imgThumb.animate().alpha(1).start();
    }
  }

  public List<TestItem.NormalVideo> getTestVideo() {
    List<TestItem.NormalVideo> testList = new ArrayList<>();
    TestItem.NormalVideo normalVideo = new TestItem.NormalVideo();
    normalVideo.videoUrl =
        "http://txmov2.a.yximgs.com/upic/2019/07/19/12/BMjAxOTA3MTkxMjU3MjlfNTY3OTU0NTk3XzE1MzYyNzM5ODQ2XzBfMw==_b_Bd2fc488a8ab090d1c3465148377170d3.mp4?tag=1-1575252564-unknown-0-aaf0c6fdzc-b504707e26db6a3b";
    normalVideo.coverUrl =
        "https://ali2.a.yximgs.com/bs2/multicover/253195749553086557.jpg";
    normalVideo.appIconUrl =
        "http://static.yximgs.com/udata/pkg/156617a461be4bb18967e87978c3341b.jpg";
    testList.add(normalVideo);
    testList.add(normalVideo);
    return testList;
  }

  private static class DrawRecyclerAdapter extends RecyclerView.Adapter {
    private Context mContext;
    private List<TestItem> mDataList;

    DrawRecyclerAdapter(Context context, List<TestItem> dataList) {
      this.mContext = context;
      this.mDataList = dataList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      switch (viewType) {
        case ItemViewType.ITEM_VIEW_TYPE_AD:
          return new DrawViewHolder(
              LayoutInflater.from(mContext).inflate(R.layout.draw_draw_item_view, parent, false));
        case ItemViewType.ITEM_VIEW_TYPE_NORMAL:
        default:
          return new NormalViewHolder(
              LayoutInflater.from(mContext).inflate(R.layout.draw_normal_item_view, parent, false));
      }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
      TestItem item = mDataList.get(position);
      if (item == null) {
        return;
      }
      if (viewHolder instanceof NormalViewHolder) {
        NormalViewHolder normalViewHolder = (NormalViewHolder) viewHolder;
        bindNormalVideoView(normalViewHolder, item.normalVideo);
      } else if (viewHolder instanceof DrawViewHolder) {
        DrawViewHolder drawViewHolder = (DrawViewHolder) viewHolder;
        KsDrawAd ksDrawAd = item.ksDrawAd;
        ksDrawAd.setAdInteractionListener(new KsDrawAd.AdInteractionListener() {
          @Override
          public void onAdClicked() {
            ToastUtil.showToast(mContext, "广告点击回调");
            LogUtils.recodeCallback(LogUtils.SCENE_DRAW_PRE, "onAdClicked");
          }

          @Override
          public void onAdShow() {
            ToastUtil.showToast(mContext, "广告曝光回调");
            LogUtils.recodeCallback(LogUtils.SCENE_DRAW_PRE, "onAdShow");
          }

          @Override
          public void onVideoPlayStart() {
            ToastUtil.showToast(mContext, "广告视频开始播放");
            LogUtils.recodeCallback(LogUtils.SCENE_DRAW_PRE, "onVideoPlayStart");

          }

          @Override
          public void onVideoPlayPause() {
            ToastUtil.showToast(mContext, "广告视频暂停播放");
            LogUtils.recodeCallback(LogUtils.SCENE_DRAW_PRE, "onVideoPlayPause");
          }

          @Override
          public void onVideoPlayResume() {
            ToastUtil.showToast(mContext, "广告视频恢复播放");
            LogUtils.recodeCallback(LogUtils.SCENE_DRAW_PRE, "onVideoPlayResume");
          }

          @Override
          public void onVideoPlayEnd() {
            ToastUtil.showToast(mContext, "广告视频播放结束");
            LogUtils.recodeCallback(LogUtils.SCENE_DRAW_PRE, "onVideoPlayEnd");
          }

          @Override
          public void onVideoPlayError() {
            ToastUtil.showToast(mContext, "广告视频播放出错");
            LogUtils.recodeCallback(LogUtils.SCENE_DRAW_PRE, "onVideoPlayError");
          }
        });

        View drawVideoView = ksDrawAd.getDrawView(mContext);
        //设置draw 视频声音播放
        ksDrawAd.setVideoSoundEnable(true);
        if (drawVideoView != null && drawVideoView.getParent() == null) {
          drawViewHolder.mVideoContainer.removeAllViews();
          drawViewHolder.mVideoContainer.addView(drawVideoView);
        }
      }
    }

    private void bindNormalVideoView(NormalViewHolder normalViewHolder,
        TestItem.NormalVideo normalVideo) {
      normalViewHolder.videoView.setVideoURI(Uri.parse(normalVideo.videoUrl));
      Glide.with(mContext).load(normalVideo.coverUrl)
//          .apply(RequestOptions.bitmapTransform(new CircleCrop()))
          .into(normalViewHolder.videoThumb);
      normalViewHolder.videoThumb.setVisibility(View.VISIBLE);
      Glide.with(mContext).load(normalVideo.appIconUrl)
//          .apply(RequestOptions.bitmapTransform(new CircleCrop()))
          .into(normalViewHolder.authorIcon);
    }

    @Override
    public int getItemCount() {
      return mDataList.size();
    }

    @Override
    public int getItemViewType(int position) {
      TestItem item = mDataList.get(position);
      if (item.isAdVideoView()) {
        return ItemViewType.ITEM_VIEW_TYPE_AD;
      } else {
        return ItemViewType.ITEM_VIEW_TYPE_NORMAL;
      }
    }

    @IntDef({ItemViewType.ITEM_VIEW_TYPE_NORMAL, ItemViewType.ITEM_VIEW_TYPE_AD})
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface ItemViewType {
      int ITEM_VIEW_TYPE_NORMAL = 0;
      int ITEM_VIEW_TYPE_AD = 1;
    }
  }

  private static class NormalViewHolder extends RecyclerView.ViewHolder {
    private VideoView videoView;
    private ImageView videoThumb;
    private ImageView authorIcon;

    NormalViewHolder(View itemView) {
      super(itemView);
      videoView = itemView.findViewById(R.id.video_view);
      videoThumb = itemView.findViewById(R.id.video_thumb);
      authorIcon = itemView.findViewById(R.id.author_icon);
    }
  }

  private static class DrawViewHolder extends RecyclerView.ViewHolder {
    private ViewGroup mVideoContainer;

    DrawViewHolder(View itemView) {
      super(itemView);
      mVideoContainer = itemView.findViewById(R.id.video_container);
    }
  }

  private static class TestItem {
    private NormalVideo normalVideo;
    private KsDrawAd ksDrawAd;

    TestItem(NormalVideo normalVideo, KsDrawAd ksDrawAd) {
      this.normalVideo = normalVideo;
      this.ksDrawAd = ksDrawAd;
    }

    boolean isAdVideoView() {
      return ksDrawAd != null;
    }

    private static class NormalVideo {
      private String videoUrl;
      private String coverUrl;
      private String appIconUrl;

      NormalVideo() {}
    }
  }
}
