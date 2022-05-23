package com.ewell.filters.auth;

import org.springframework.util.DigestUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SignUtils.
 */
public final class SignUtils {

    private static final SignUtils SIGN_UTILS = new SignUtils();

    private SignUtils() {
    }

    /**
     * getInstance.
     *
     * @return {@linkplain SignUtils}
     */
    public static SignUtils getInstance() {
        return SIGN_UTILS;
    }

    /**
     * acquired sign.
     *
     * @param signKey sign key
     * @param params  params
     * @return sign
     */
    public static String generateSign(final String signKey, final Map<String, String> params) {
        List<String> storedKeys = Arrays.stream(params.keySet()
                .toArray(new String[]{}))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        final String sign = storedKeys.stream()
                .filter(key -> !Objects.equals(key, "sign"))
                .map(key -> String.join("", key, params.get(key)))
                .collect(Collectors.joining()).trim()
                .concat(signKey);
        // TODO this is a risk for error charset coding with getBytes
        return DigestUtils.md5DigestAsHex(sign.getBytes()).toUpperCase();
    }

    /**
     * isValid.
     *
     * @param sign    sign
     * @param params  params
     * @param signKey signKey
     * @return boolean boolean
     */
    public boolean isValid(final String sign, final Map<String, String> params, final String signKey) {
        return Objects.equals(sign, generateSign(signKey, params));
    }

    /**
     * Generate key string.
     *
     * @return the string
     */
    public String generateKey() {
        return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
    }

}
