package com.ewell.spi;

import java.lang.annotation.*;

/**
 * Join
 * Adding this annotation to a class indicates joining the extension mechanism.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Join {
}
