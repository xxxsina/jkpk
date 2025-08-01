package com.kwad.demo.open;

import com.kwad.demo.open.utils.TestPermissionUtil;
import com.kwad.demo.open.utils.TestSpUtil;
import com.kwad.sdk.api.KsCustomController;

/**
 * 非必选参数
 * 控制SDK获取设备信息的测试接口，媒体根据自身诉求继承KsCustomController，重写相关方法控制即可
 */
public class UserDataObtainController extends KsCustomController {
  private boolean userAgree;

  private UserDataObtainController() {
    userAgree = TestPermissionUtil.isUserAgreePrivacy();
  }

  private static class Holder {
    private static final UserDataObtainController sInstance = new UserDataObtainController();
  }

  public static UserDataObtainController getInstance() {
    return Holder.sInstance;
  }

  public void setUserAgree(boolean userAgree) {
    this.userAgree = userAgree;
  }

  @Override
  public boolean canReadLocation() {
    // 为提高广告转化率，取得更好收益，建议媒体在用户同意隐私政策及权限信息后，允许SDK获取地理位置信息。
    return userAgree;
  }

  @Override
  public boolean canUsePhoneState() {
    // 为提高广告转化率，取得更好收益，建议媒体在用户同意隐私政策及权限信息后，允许SDK使用手机硬件信息。
    return userAgree;
  }

  @Override
  public boolean canUseOaid() {
    // 为提高广告转化率，取得更好收益，建议媒体在用户同意隐私政策及权限信息后，允许SDK使用设备oaid。
    return userAgree;
  }

  @Override
  public boolean canUseMacAddress() {
    // 为提高广告转化率，取得更好收益，建议媒体在用户同意隐私政策及权限信息后，允许SDK使用设备Mac地址。
    return userAgree;
  }

  @Override
  public boolean canReadInstalledPackages() {
    // 为提高广告转化率，取得更好收益，建议媒体在用户同意隐私政策及权限信息后，允许SDK读取app安装列表。
    return userAgree;
  }

  @Override
  public boolean canUseStoragePermission() {
    // 为提升SDK的接入体验，广告展示更流畅，建议媒体在用户同意隐私政策及权限信息后，允许SDK使用存储权限。
    return userAgree;
  }

  @Override
  public boolean canUseNetworkState() {
    // 为提升SDK的接入体验，广告展示更流畅，建议媒体在用户同意隐私政策及权限信息后，允许SDK读取网络状态信息。
    return userAgree;
  }
}
