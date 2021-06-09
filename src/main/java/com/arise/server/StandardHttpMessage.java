package com.arise.server;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.rtsp.RtspResponseStatuses;

/**
 * @Author: wy
 * @Date: Created in 14:54 2021-06-05
 * @Description:
 * @Modified: By：
 */
public interface StandardHttpMessage {

    DefaultHttpResponse Established =
            new DefaultHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "Connection Established"));

    DefaultHttpResponse _404 =
            new DefaultHttpResponse(HttpVersion.HTTP_1_1, RtspResponseStatuses.NOT_FOUND);

    DefaultHttpResponse _500 =
            new DefaultHttpResponse(HttpVersion.HTTP_1_1, RtspResponseStatuses.INTERNAL_SERVER_ERROR);
}
