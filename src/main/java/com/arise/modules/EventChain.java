package com.arise.modules;

import java.util.LinkedList;

/**
 * @Author: wy
 * @Date: Created in 15:41 2021-02-24
 * @Description:
 * @Modified: By：
 */
public class EventChain {

    private LinkedList<EventHandler> chain = new LinkedList<>();

    public void addHandler(EventHandler handler) {
        chain.add(handler);
    }

}
