package com.kwad.demo.open.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.ListView;

public class LoadMoreListView extends ListView implements AbsListView.OnScrollListener {
  private int mLastVisibleItem, mTotalItemCount;
  private boolean mIsLoading = false;
  private ILoadMoreListener mLoadMoreListener;
  private LoadMoreView mLoadMoreView;

  public LoadMoreListView(Context context) {
    this(context, null);
  }

  public LoadMoreListView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public LoadMoreListView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    setOnScrollListener(this);
    mLoadMoreView = new LoadMoreView(getContext());
    addFooterView(mLoadMoreView);
  }

  public void setLoadMoreListener(ILoadMoreListener loadMoreListener) {
    mLoadMoreListener = loadMoreListener;
  }

  public void setLoadingFinish() {
    mIsLoading = false;
  }

  public void setLoadingError() {
    mIsLoading = false;
    mLoadMoreView.showError();
  }

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {
    if (mLastVisibleItem == mTotalItemCount && scrollState == SCROLL_STATE_IDLE) {
      if (mLoadMoreListener != null && !mIsLoading) {
        mIsLoading = true;
        mLoadMoreView.showLoading();
        mLoadMoreListener.onLoadMore();
      }
    }
  }

  @Override
  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
      int totalItemCount) {
    mLastVisibleItem = firstVisibleItem + visibleItemCount;
    mTotalItemCount = totalItemCount;
  }

  public interface ILoadMoreListener {
    void onLoadMore();
  }
}
