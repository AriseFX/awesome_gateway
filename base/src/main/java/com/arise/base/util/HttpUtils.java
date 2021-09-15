package com.arise.base.util;

import com.arise.base.exception.GatewayException;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

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
}
