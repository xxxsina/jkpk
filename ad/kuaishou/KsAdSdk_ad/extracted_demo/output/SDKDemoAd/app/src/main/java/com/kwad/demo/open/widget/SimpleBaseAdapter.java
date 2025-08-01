package com.kwad.demo.open.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.widget.BaseAdapter;

/**
 * listView 的通用adapter ，封装了常用的数据set，add、remove 逻辑
 * @param <T>
 */
public abstract class SimpleBaseAdapter<T> extends BaseAdapter {
  protected List<T> mDatas;

  public SimpleBaseAdapter() {
  }

  public SimpleBaseAdapter(List<T> dataList) {
    mDatas = dataList;
  }

  public void setListData(List<T> dataList) {
    mDatas = dataList;
  }

  public void addData(T data) {
    if (mDatas == null) {
      mDatas = new ArrayList<T>();
    }
    mDatas.add(data);
  }

  public boolean addDataUnique(T data) {
    if (mDatas == null) {
      mDatas = new ArrayList<T>();
    }
    if (!mDatas.contains(data)) {
      mDatas.add(data);
      return true;
    }
    return false;
  }

  public boolean containData(T data) {
    if (mDatas != null) {
      return mDatas.contains(data);
    }
    return false;
  }

  public void addData(T data, int position) {
    if (mDatas == null) {
      mDatas = new ArrayList<T>();
    }
    if (position < 0 || position > mDatas.size()) {
      mDatas.add(data);
    } else {
      mDatas.add(position, data);
    }

  }

  public boolean removeData(T data) {
    if (mDatas != null) {
      return mDatas.remove(data);
    }
    return false;
  }

  public void clearData() {
    if (mDatas != null) {
      mDatas.clear();
    }
  }

  public T removeData(int position) {
    if (mDatas != null) {
      return mDatas.remove(position);
    }
    return null;
  }

  public <E> void addAll(Collection<E> collection) {
    if (collection == null || collection.size() == 0) {
      return;
    }
    if (mDatas == null) {
      mDatas = new ArrayList<T>();
    }
    mDatas.addAll((Collection<? extends T>) collection);
  }


  public <E> void addAll(Collection<E> collection, int position) {
    if (collection == null || collection.size() == 0) {
      return;
    }
    if (mDatas == null) {
      mDatas = new ArrayList<T>();
    }
    mDatas.addAll(position, (Collection<? extends T>) collection);
  }

  public List<T> getAllData() {
    return mDatas;
  }

  @Override
  public int getCount() {
    if (mDatas != null) {
      return mDatas.size();
    }
    return 0;
  }


  @Override
  public T getItem(int position) {
    if (mDatas != null) {
      if (position >= 0 && position < mDatas.size()) {
        return mDatas.get(position);
      }
    }
    return null;
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

}
