package com.kwad.demo.open.draw.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ViewPagerLayoutManager extends LinearLayoutManager
    implements
      RecyclerView.OnChildAttachStateChangeListener {

  private PagerSnapHelper mPagerSnapHelper;
  private OnViewPagerListener mOnViewPagerListener;
  private int mDrift;// 位移，用来判断移动方向

  {
    mPagerSnapHelper = new PagerSnapHelper();
  }

  public ViewPagerLayoutManager(Context context, int orientation) {
    this(context, orientation, false);
  }

  public ViewPagerLayoutManager(Context context, int orientation, boolean reverseLayout) {
    super(context, orientation, reverseLayout);
  }

  @Override
  public void onAttachedToWindow(RecyclerView recyclerView) {
    super.onAttachedToWindow(recyclerView);
    mPagerSnapHelper.attachToRecyclerView(recyclerView);
    recyclerView.addOnChildAttachStateChangeListener(this);
  }

  @Override
  public void onScrollStateChanged(int state) {
    if (state == RecyclerView.SCROLL_STATE_IDLE) {
      View viewIdle = mPagerSnapHelper.findSnapView(ViewPagerLayoutManager.this);
      if (viewIdle == null) {
        return;
      }
      int positionIdle = getPosition(viewIdle);
      if (mOnViewPagerListener != null && getChildCount() == 1) {
        mOnViewPagerListener.onPageSelected(positionIdle, positionIdle == getItemCount() - 1);
      }
    }
  }

  /**
   * 监听竖直方向的相对偏移量
   */
  @Override
  public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
    this.mDrift = dy;
    return super.scrollVerticallyBy(dy, recycler, state);
  }


  /**
   * 监听水平方向的相对偏移量
   */
  @Override
  public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
      RecyclerView.State state) {
    this.mDrift = dx;
    return super.scrollHorizontallyBy(dx, recycler, state);
  }

  /**
   * 设置监听
   */
  public void setOnViewPagerListener(OnViewPagerListener listener) {
    this.mOnViewPagerListener = listener;
  }

  @Override
  public void onChildViewAttachedToWindow(@NonNull View view) {
    if (mOnViewPagerListener != null && getChildCount() == 1) {
      mOnViewPagerListener.onInitComplete();
    }
  }

  @Override
  public void onChildViewDetachedFromWindow(@NonNull View view) {
    if (mDrift >= 0) {
      if (mOnViewPagerListener != null)
        mOnViewPagerListener.onPageRelease(true, getPosition(view));
    } else {
      if (mOnViewPagerListener != null)
        mOnViewPagerListener.onPageRelease(false, getPosition(view));
    }
  }
}
