package com.kwad.demo.open.utils;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.CharacterStyle;

/**
 * 生成带样式的字符串的工具类
 */
public class SpannableStringBuilderUtil {

  private final SpannableStringBuilder sb;
  private SpannableString currentSpan;

  public SpannableStringBuilderUtil() {
    sb = new SpannableStringBuilder();
  }

  /**
   * 获取设置完的字符串
   */
  public SpannableStringBuilder build() {
    if (currentSpan != null) {
      sb.append(currentSpan);
    }
    return sb;
  }

  /**
   * 添加一个待设置样式的字符串
   */
  public SpannableStringBuilderUtil append(CharSequence source) {
    if (currentSpan != null) {
      sb.append(currentSpan);
    }
    if (!TextUtils.isEmpty(source)) {
      currentSpan = new SpannableString(source);
    }
    return this;
  }

  /**
   * 给上一次append进来的字符串设置样式
   */
  public SpannableStringBuilderUtil setSpan(CharacterStyle span) {
    if (currentSpan != null && span != null) {
      currentSpan.setSpan(span, 0, currentSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    return this;
  }
}
