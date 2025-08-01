package com.kwad.demo.open;

public enum TestPosId {
  /** 广告联盟测试PosId **/
  POSID_REWARD(90009001), // 激励视频测试PosId
  POSID_FULLSCREEN(90009002), // 全屏视频测试PosId
  POSID_INTERSTITIAL(4000000276L), // 插屏视频测试PosId

  POS_ID_BANNER(4000001623L), // Banner广告测试PosId
  POS_ID_BANNER_1(4000001624L), // Banner广告测试PosId
  POS_ID_BANNER_2(4000001827L), // Banner广告测试PosId
  POS_ID_BANNER_3(4000001828L), // Banner广告测试PosId
  POS_ID_NEW_INTERSTITIAL_FULL(4000001587L), // 新插屏广告-全屏
  POS_ID_NEW_INTERSTITIAL_HALF(4000001588L), // 新插屏广告-半屏
  POS_ID_NEW_INTERSTITIAL(4000001592L), // 新插屏广告-优选


  POSID_DRAW(4000000020L), // Draw信息流测试PosId
  POSID_NATIVE_VIDEO(4000000021L), // Native视频测试PosId
  POSID_NATIVE_IMAGE(90009004), // 图文测试PosId

  POSID_NATIVE_INTERSTITIAL(4000001473L),//插屏自渲染
  POSID_FEED_TYPE_1(4000001377L), // 自渲染listView

  /** 自定义信息流模版PosId **/
  POSID_CONFIG_FEED_TYPE_1(4000000075L), // 自定义Feed测试+文字悬浮在图片
  POSID_CONFIG_FEED_TYPE_2(4000000078L), // 自定义Feed测试+左文右图
  POSID_CONFIG_FEED_TYPE_3(4000000076L), // 自定义Feed测试+左图右文
  POSID_CONFIG_FEED_TYPE_4(4000000074L), // 自定义Feed测试+上文下图/上文下视频
  POSID_CONFIG_FEED_TYPE_5(4000000079L), // 自定义Feed测试+上图下文/上视频下文
  POSID_CONFIG_FEED_TYPE_9(4000001342L), // 自定义Feed测试+三图样式
  POSID_CONFIG_FEED_TYPE_10(4000001347L), // 自定义Feed测试+橱窗样式
  POSID_CONFIG_FEED_TYPE_11(4000001348L), // 自定义Feed测试+竖版上文下图  （支持竖版视频、图片）
  POSID_CONFIG_FEED_TYPE_12(4000001349L), // 自定义Feed测试+竖版上图下文  （支持竖版视频、图片）
  POSID_CONFIG_FEED_TYPE_13(4000001350L), // 自定义Feed测试+竖版大图     （支持竖版视频、图片）
  POSID_CONFIG_FEED_TYPE_14(4000001700L), // 自定义Feed测试+竖版小说样式     （支持竖版视频、图片）
  POSID_CONFIG_FEED_TYPE_15(4000001701L), // 自定义Feed测试+竖版双列样式     （支持竖版视频、图片）

  POSID_SPLASHSCREEN(4000000042L), // 开屏视频测试PosId

  POSID_SPLASHSCREEN_LANDSCAPE(4000001576L), // 开屏适配横屏样式测试PosId


  /** 内容联盟测试PosId **/
  POSID_ENTRY_TYPE1(90009005), // 入口组件测试id 样式1
  POSID_ENTRY_TYPE2(4000000022L), // 入口组件测试id 样式 2；
  POSID_ENTRY_TYPE3(4000000026L), // 入口组件测试id 样式3
  POSID_ENTRY_TYPE4(4000000027L), // 入口组件测试id 样式 4；
  POSID_ENTRY_TYPE5(4000000058L), // 入口组件测试id 样式 5 Tab全场景类样式
  POSID_CONTENT_PAGE(90009005L), // 内容联盟滑滑流测试PosId
  POSID_FEED_PAGE_1(4000000062L), // 内容联盟双Feed流测试PosId
  POSID_FEED_PAGE_2(4000000066L), // 内容联盟单Feed流小卡样式测试PosId
  POSID_FEED_PAGE_3(4000000068L), // 内容联盟双Feed流大卡样式测试PosId
  POSID_HOTSPOT_PAGE(90009005L), // 热点列表组件测试id
  POSID_TUBE_PAGE(90009005L), // 内容联盟短剧测试id
  /** 横版视频posId **/
  POSID_HORIZONTAL_FEED_PAGE(4000000082L), // 内容联盟横版视频PosId
  POSID_HORIZONTAL_IMAGE_PAGE(4000000302L), // 内容联盟横版视频+图文PosId

  /** 电商联盟测试PosId **/
  POSID_1(90009), // 直播入口id
  POSID_2(4000000290L); // 内容入口id


  public long posId;

  TestPosId(long posId) {
    this.posId = posId;
  }
}
