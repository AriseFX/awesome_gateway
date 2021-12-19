package com.ewell.common.util;

import com.ewell.common.exception.GatewayException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

/**
 * @Author: wy
 * @Date: Created in 17:08 2021-07-14
 * @Description: http解析相关
 * @Modified: By：
 */
public class HttpUtils {

    /**
     * 解析http query parameter
     * ex:  a=1&b=2
     */
    public static Map<String, String> parseQueryString(String s) {
        if (s == null) {
            return null;
        }
        HashMap<String, String> ht = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(s, "&");
        while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            int pos = pair.indexOf('=');
            if (pos == -1) {
                // XXX
                // should give more detail about the illegal argument
                throw new GatewayException("query参数解析出错");
            }
            String key = parseName(pair.substring(0, pos), sb);
            String val = parseName(pair.substring(pos + 1), sb);
            ht.put(key, val);
        }
        return ht;
    }

    private static String parseName(String s, StringBuilder sb) {
        sb.setLength(0);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    sb.append(' ');
                    break;
                case '%':
                    try {
                        sb.append((char) Integer.parseInt(s.substring(i + 1, i + 3),
                                16));
                        i += 2;
                    } catch (NumberFormatException e) {
                        // XXX
                        // need to be more specific about illegal arg
                        throw new IllegalArgumentException();
                    } catch (StringIndexOutOfBoundsException e) {
                        String rest = s.substring(i);
                        sb.append(rest);
                        if (rest.length() == 2)
                            i++;
                    }

                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }


    public static byte[] unCompressGzip(byte[] in, int offset, int len) {
        ByteArrayOutputStream out = null;
        GZIPInputStream gunzip = null;
        try {
            if (in == null || len == 0) {
                return new byte[0];
            }
            out = new ByteArrayOutputStream();
            gunzip = new GZIPInputStream(new ByteArrayInputStream(in, offset, len));
            byte[] buffer = new byte[1024];
            int n;
            while ((n = gunzip.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            out.flush();
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (gunzip != null) {
                    gunzip.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
