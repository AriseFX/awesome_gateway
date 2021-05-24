package com.arise.modules.http.constant;

import io.netty.util.AsciiString;

/**
 * @Author: wy
 * @Date: Created in 14:29 2021-05-24
 * @Description:
 * @Modified: By：
 */
public class HttpStatusConstant {

    /**
     * 100 Continue
     */
    public static final AsciiString CONTINUE = AsciiString.cached("100 Continue");

    /**
     * 101 Switching Protocols
     */
    public static final AsciiString SWITCHING_PROTOCOLS = AsciiString.cached("101 Switching Protocols");

    /**
     * 102 Processing (WebDAV, RFC2518)
     */
    public static final AsciiString PROCESSING = AsciiString.cached("102 Processing");

    /**
     * 200 OK
     */
    public static final AsciiString OK = AsciiString.cached("200 OK");

    /**
     * 201 Created
     */
    public static final AsciiString CREATED = AsciiString.cached("201 Created");

    /**
     * 202 Accepted
     */
    public static final AsciiString ACCEPTED = AsciiString.cached("202 Accepted");

    /**
     * 203 Non-Authoritative Information (since HTTP/1.1)
     */
    public static final AsciiString NON_AUTHORITATIVE_INFORMATION =
            AsciiString.cached("203 Non-Authoritative Information");

    /**
     * 204 No Content
     */
    public static final AsciiString NO_CONTENT = AsciiString.cached("204 No Content");

    /**
     * 205 Reset Content
     */
    public static final AsciiString RESET_CONTENT = AsciiString.cached("205 Reset Content");

    /**
     * 206 Partial Content
     */
    public static final AsciiString PARTIAL_CONTENT = AsciiString.cached("206 Partial Content");

    /**
     * 207 Multi-Status (WebDAV, RFC2518)
     */
    public static final AsciiString MULTI_STATUS = AsciiString.cached("207 Multi-Status");

    /**
     * 300 Multiple Choices
     */
    public static final AsciiString MULTIPLE_CHOICES = AsciiString.cached("300 Multiple Choices");

    /**
     * 301 Moved Permanently
     */
    public static final AsciiString MOVED_PERMANENTLY = AsciiString.cached("301 Moved Permanently");

    /**
     * 302 Found
     */
    public static final AsciiString FOUND = AsciiString.cached("302 Found");

    /**
     * 303 See Other (since HTTP/1.1)
     */
    public static final AsciiString SEE_OTHER = AsciiString.cached("303 See Other");

    /**
     * 304 Not Modified
     */
    public static final AsciiString NOT_MODIFIED = AsciiString.cached("304 Not Modified");

    /**
     * 305 Use Proxy (since HTTP/1.1)
     */
    public static final AsciiString USE_PROXY = AsciiString.cached("305 Use Proxy");

    /**
     * 307 Temporary Redirect (since HTTP/1.1)
     */
    public static final AsciiString TEMPORARY_REDIRECT = AsciiString.cached("307 Temporary Redirect");

    /**
     * 308 Permanent Redirect (RFC7538)
     */
    public static final AsciiString PERMANENT_REDIRECT = AsciiString.cached("308 Permanent Redirect");

    /**
     * 400 Bad Request
     */
    public static final AsciiString BAD_REQUEST = AsciiString.cached("400 Bad Request");

    /**
     * 401 Unauthorized
     */
    public static final AsciiString UNAUTHORIZED = AsciiString.cached("401 Unauthorized");

    /**
     * 402 Payment Required
     */
    public static final AsciiString PAYMENT_REQUIRED = AsciiString.cached("402 Payment Required");

    /**
     * 403 Forbidden
     */
    public static final AsciiString FORBIDDEN = AsciiString.cached("403 Forbidden");

    /**
     * 404 Not Found
     */
    public static final AsciiString NOT_FOUND = AsciiString.cached("404 Not Found");

    /**
     * 405 Method Not Allowed
     */
    public static final AsciiString METHOD_NOT_ALLOWED = AsciiString.cached("405 Method Not Allowed");

    /**
     * 406 Not Acceptable
     */
    public static final AsciiString NOT_ACCEPTABLE = AsciiString.cached("406 Not Acceptable");

    /**
     * 407 Proxy Authentication Required
     */
    public static final AsciiString PROXY_AUTHENTICATION_REQUIRED =
            AsciiString.cached("407 Proxy Authentication Required");

    /**
     * 408 Request Timeout
     */
    public static final AsciiString REQUEST_TIMEOUT = AsciiString.cached("408 Request Timeout");

    /**
     * 409 Conflict
     */
    public static final AsciiString CONFLICT = AsciiString.cached("409 Conflict");

    /**
     * 410 Gone
     */
    public static final AsciiString GONE = AsciiString.cached("410 Gone");

    /**
     * 411 Length Required
     */
    public static final AsciiString LENGTH_REQUIRED = AsciiString.cached("411 Length Required");

    /**
     * 412 Precondition Failed
     */
    public static final AsciiString PRECONDITION_FAILED = AsciiString.cached("412 Precondition Failed");

    /**
     * 413 Request Entity Too Large
     */
    public static final AsciiString REQUEST_ENTITY_TOO_LARGE =
            AsciiString.cached("413 Request Entity Too Large");

    /**
     * 414 Request-URI Too Long
     */
    public static final AsciiString REQUEST_URI_TOO_LONG = AsciiString.cached("414 Request-URI Too Long");

    /**
     * 415 Unsupported Media Type
     */
    public static final AsciiString UNSUPPORTED_MEDIA_TYPE = AsciiString.cached("415 Unsupported Media Type");

    /**
     * 416 Requested Range Not Satisfiable
     */
    public static final AsciiString REQUESTED_RANGE_NOT_SATISFIABLE =
            AsciiString.cached("416 Requested Range Not Satisfiable");

    /**
     * 417 Expectation Failed
     */
    public static final AsciiString EXPECTATION_FAILED = AsciiString.cached("417 Expectation Failed");

    /**
     * 421 Misdirected Request
     *
     * @see <a href="https://tools.ietf.org/html/rfc7540#section-9.1.2">421 (Misdirected Request) Status Code</a>
     */
    public static final AsciiString MISDIRECTED_REQUEST = AsciiString.cached("421 Misdirected Request");

    /**
     * 422 Unprocessable Entity (WebDAV, RFC4918)
     */
    public static final AsciiString UNPROCESSABLE_ENTITY = AsciiString.cached("422 Unprocessable Entity");

    /**
     * 423 Locked (WebDAV, RFC4918)
     */
    public static final AsciiString LOCKED = AsciiString.cached("423 Locked");

    /**
     * 424 Failed Dependency (WebDAV, RFC4918)
     */
    public static final AsciiString FAILED_DEPENDENCY = AsciiString.cached("424 Failed Dependency");

    /**
     * 425 Unordered Collection (WebDAV, RFC3648)
     */
    public static final AsciiString UNORDERED_COLLECTION = AsciiString.cached("425 Unordered Collection");

    /**
     * 426 Upgrade Required (RFC2817)
     */
    public static final AsciiString UPGRADE_REQUIRED = AsciiString.cached("426 Upgrade Required");

    /**
     * 428 Precondition Required (RFC6585)
     */
    public static final AsciiString PRECONDITION_REQUIRED = AsciiString.cached("428 Precondition Required");

    /**
     * 429 Too Many Requests (RFC6585)
     */
    public static final AsciiString TOO_MANY_REQUESTS = AsciiString.cached("429 Too Many Requests");

    /**
     * 431 Request Header Fields Too Large (RFC6585)
     */
    public static final AsciiString REQUEST_HEADER_FIELDS_TOO_LARGE =
            AsciiString.cached("431 Request Header Fields Too Large");

    /**
     * 500 Internal Server Error
     */
    public static final AsciiString INTERNAL_SERVER_ERROR = AsciiString.cached("500 Internal Server Error");

    /**
     * 501 Not Implemented
     */
    public static final AsciiString NOT_IMPLEMENTED = AsciiString.cached("501 Not Implemented");

    /**
     * 502 Bad Gateway
     */
    public static final AsciiString BAD_GATEWAY = AsciiString.cached("502 Bad Gateway");

    /**
     * 503 Service Unavailable
     */
    public static final AsciiString SERVICE_UNAVAILABLE = AsciiString.cached("503 Service Unavailable");

    /**
     * 504 Gateway Timeout
     */
    public static final AsciiString GATEWAY_TIMEOUT = AsciiString.cached("504 Gateway Timeout");

    /**
     * 505 HTTP Version Not Supported
     */
    public static final AsciiString HTTP_VERSION_NOT_SUPPORTED =
            AsciiString.cached("505 HTTP Version Not Supported");

    /**
     * 506 Variant Also Negotiates (RFC2295)
     */
    public static final AsciiString VARIANT_ALSO_NEGOTIATES = AsciiString.cached("506 Variant Also Negotiates");

    /**
     * 507 Insufficient Storage (WebDAV, RFC4918)
     */
    public static final AsciiString INSUFFICIENT_STORAGE = AsciiString.cached("507 Insufficient Storage");

    /**
     * 510 Not Extended (RFC2774)
     */
    public static final AsciiString NOT_EXTENDED = AsciiString.cached("510 Not Extended");

    /**
     * 511 Network Authentication Required (RFC6585)
     */
    public static final AsciiString NETWORK_AUTHENTICATION_REQUIRED = AsciiString.cached("511 Network Authentication Required");
}
