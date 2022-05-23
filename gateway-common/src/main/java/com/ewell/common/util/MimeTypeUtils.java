/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ewell.common.util;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Miscellaneous utility methods.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Dimitrios Liapis
 * @author Sam Brannen
 * @since 4.0
 * @Modified wy
 */
public abstract class MimeTypeUtils {

    private static final BitSet TOKEN;

    static {
        // variable names refer to RFC 2616, section 2.2
        BitSet ctl = new BitSet(128);
        for (int i = 0; i <= 31; i++) {
            ctl.set(i);
        }
        ctl.set(127);

        BitSet separators = new BitSet(128);
        separators.set('(');
        separators.set(')');
        separators.set('<');
        separators.set('>');
        separators.set('@');
        separators.set(',');
        separators.set(';');
        separators.set(':');
        separators.set('\\');
        separators.set('\"');
        separators.set('/');
        separators.set('[');
        separators.set(']');
        separators.set('?');
        separators.set('=');
        separators.set('{');
        separators.set('}');
        separators.set(' ');
        separators.set('\t');

        TOKEN = new BitSet(128);
        TOKEN.set(0, 128);
        TOKEN.andNot(ctl);
        TOKEN.andNot(separators);
    }


    /**
     * Parse the given String into a single {@code MimeType}.
     *
     * @param mimeType the string to parse
     * @return the subtype
     */
    public static String parseMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        int index = mimeType.indexOf(';');
        String fullType = (index >= 0 ? mimeType.substring(0, index) : mimeType).trim();
        if (fullType.isEmpty()) {
            return null;
        }
        // java.net.HttpURLConnection returns a *; q=.2 Accept header
        if ("*".equals(fullType)) {
            fullType = "*/*";
        }
        int subIndex = fullType.indexOf('/');
        if (subIndex == -1) {
            return null;
        }
        if (subIndex == fullType.length() - 1) {
            return null;
        }
        String type = fullType.substring(0, subIndex);
        String subtype = fullType.substring(subIndex + 1, fullType.length());
        if ("*".equals(type) && !"*".equals(subtype)) {
            return null;
        }

        Map<String, String> parameters = null;
        do {
            int nextIndex = index + 1;
            boolean quoted = false;
            while (nextIndex < mimeType.length()) {
                char ch = mimeType.charAt(nextIndex);
                if (ch == ';') {
                    if (!quoted) {
                        break;
                    }
                } else if (ch == '"') {
                    quoted = !quoted;
                }
                nextIndex++;
            }
            String parameter = mimeType.substring(index + 1, nextIndex).trim();
            if (parameter.length() > 0) {
                if (parameters == null) {
                    parameters = new LinkedHashMap<>(4);
                }
                int eqIndex = parameter.indexOf('=');
                if (eqIndex >= 0) {
                    String attribute = parameter.substring(0, eqIndex).trim();
                    String value = parameter.substring(eqIndex + 1, parameter.length()).trim();
                    parameters.put(attribute, value);
                }
            }
            index = nextIndex;
        }
        while (index < mimeType.length());
        if (checkToken(subtype)) {
            return subtype;
        }
        return null;
    }

    /**
     * Checks the given token string for illegal characters, as defined in RFC 2616,
     * section 2.2.
     *
     * @throws IllegalArgumentException in case of illegal characters
     * @see <a href="https://tools.ietf.org/html/rfc2616#section-2.2">HTTP 1.1, section 2.2</a>
     */
    private static boolean checkToken(String token) {
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (!TOKEN.get(ch)) {
                return false;
            }
        }
        return true;
    }
}
