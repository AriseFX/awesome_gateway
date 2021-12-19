package com.ewell.core.filer.context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @Author: wy
 * @Date: Created in 4:48 下午 2021/11/30
 * @Description:
 * @Modified: By：
 */
@Getter
@AllArgsConstructor
public class Observer<T> implements Comparable<Observer<T>> {

    private final int order;

    private final Consumer<T> consumer;

    @Override
    public int compareTo(@NotNull Observer o) {
        return order - o.getOrder();
    }
}
