package com.kwad.demo.open.serverBid;

import java.util.List;

/**
 * 媒体adx服务端竞价完成后，快手sdk拉取广告素材时需要用的参数(需要媒体app传给快手SDK，以完成广告展示)
 */
public class BidResponse {

  public long llsid;
  public int result;
  public String errorMsg;
  public String egid;
  public String cookie;
  public boolean hasMore;
  public String extra;
  public String impAdInfo; // 一次请求方案必传参数，二次请求方案不传
  public String adxId; // 二次请求方案必传参数，一次请求方案不传
  public List<AdBid> adBids; // 竞价信息，必传

  public class AdBid {
    public long creativeId;
    public int ecpm;
    public int bidEcpm;
    public String winNoticeUrl;
    public String materialId;
  }
}
