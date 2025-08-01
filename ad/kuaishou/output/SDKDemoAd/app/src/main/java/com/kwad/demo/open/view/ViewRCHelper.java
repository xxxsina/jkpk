package com.kwad.demo.open.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.kwad.demo.open.utils.ViewUtil;

/**
 * 圆角实现辅助类
 */
public class ViewRCHelper {

  public float[] radiusArray = new float[8]; // top-left, top-right, bottom-right, bottom-left
  private float mRadius; // 圆角大小
  private Path mPath; // 裁剪路径
  private Paint mPaint; // 画笔
  private RectF mRectF; // 画布层大小
  private boolean mClipBackground; // 是否裁剪背景

  private final CornerConf mCornerConf;

  public ViewRCHelper() {
    mCornerConf = new CornerConf();
  }

  public void initAttrs(Context context, @Nullable AttributeSet attrs) {
    mRadius = ViewUtil.dip2px(context, 10);
    mClipBackground = true;
    mPath = new Path();
    mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mRectF = new RectF();
    mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
  }

  public void setRadius(float radius) {
    mRadius = radius;
  }

  public void setRadius(float[] radius) {
    radiusArray = radius;
  }

  public void onSizeChanged(int w, int h) {
    mRectF.set(0, 0, w, h);
  }

  public void beforeDraw(Canvas canvas) {
    if (!mClipBackground) {
      return;
    }
    if (Build.VERSION.SDK_INT < 28) {
      canvas.saveLayer(mRectF, null, Canvas.ALL_SAVE_FLAG);
    } else {
      canvas.save();
      canvas.clipPath(getPath());
    }
  }

  public void afterDraw(Canvas canvas) {
    if (!mClipBackground) {
      return;
    }
    if (Build.VERSION.SDK_INT < 28) {
      canvas.drawPath(getPath(), mPaint);
    }
    canvas.restore();
  }

  public void beforeDispatchDraw(Canvas canvas) {
    if (Build.VERSION.SDK_INT >= 28) {
      canvas.save();
      canvas.clipPath(getPath());
    } else {
      canvas.saveLayer(mRectF, null, Canvas.ALL_SAVE_FLAG);
    }
  }

  public void afterDispatchDraw(Canvas canvas) {
    if (Build.VERSION.SDK_INT < 28) {
      canvas.drawPath(getPath(), mPaint);
    }
    canvas.restore();
  }

  private float[] getRadius() {
    radiusArray[0] = mCornerConf.isLeftTop() ? mRadius : 0;
    radiusArray[1] = mCornerConf.isLeftTop() ? mRadius : 0;

    radiusArray[2] = mCornerConf.isTopRight() ? mRadius : 0;
    radiusArray[3] = mCornerConf.isTopRight() ? mRadius : 0;

    radiusArray[4] = mCornerConf.isRightBottom() ? mRadius : 0;
    radiusArray[5] = mCornerConf.isRightBottom() ? mRadius : 0;

    radiusArray[6] = mCornerConf.isBottomLeft() ? mRadius : 0;
    radiusArray[7] = mCornerConf.isBottomLeft() ? mRadius : 0;
    return radiusArray;
  }

  @NonNull
  public CornerConf getCornerConf() {
    return mCornerConf;
  }

  private Path getPath() {
    try {
      mPath.reset();
    } catch (Exception ignored) {

    }
    float[] radius;
    if (mRadius == 0) {
      radius = radiusArray;
    } else {
      radius = getRadius();
    }
    mPath.addRoundRect(mRectF, radius, Path.Direction.CW);
    return mPath;
  }

  /**
   * 圆角的配置，支持设置制定的角为圆角
   */
  public static class CornerConf {
    // 左上角是否开启圆角
    private boolean leftTop = true;
    // 右上角是否开启圆角
    private boolean topRight = true;
    // 右下角是否开启圆角
    private boolean rightBottom = true;
    // 左下角是否开启圆角
    private boolean bottomLeft = true;

    public CornerConf() {
    }

    public boolean isLeftTop() {
      return leftTop;
    }

    public CornerConf setLeftTop(boolean leftTop) {
      this.leftTop = leftTop;
      return this;
    }

    public boolean isTopRight() {
      return topRight;
    }

    public CornerConf setTopRight(boolean topRight) {
      this.topRight = topRight;
      return this;
    }

    public boolean isRightBottom() {
      return rightBottom;
    }

    public CornerConf setRightBottom(boolean rightBottom) {
      this.rightBottom = rightBottom;
      return this;
    }

    public boolean isBottomLeft() {
      return bottomLeft;
    }

    public CornerConf setBottomLeft(boolean bottomLeft) {
      this.bottomLeft = bottomLeft;
      return this;
    }
  }
}
