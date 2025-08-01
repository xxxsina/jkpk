package com.kwad.demo.open.serverBid;

import java.util.List;

/**
 * 媒体adx服务端通过接口从快手服务端拉取竞价信息时用的 request
 */
public class BidRequest {

  public String adxId;
  public String ip;
  public String sdkToken;
  public List<AdImpInfo> adImpInfos;

  public static class AdImpInfo {
    public long posId;
    public int cpmBidFloor;
    public int adCount;
  }
}
