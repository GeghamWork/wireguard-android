/*
 * Copyright © 2017 John Wu <topjohnwu@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package com.wireguard.android.util;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Topic {

    private static final int NON_INIT = 0;
    private static final int PENDING = 1;
    private static final int PUBLISHED = 2;

    private int state = NON_INIT;
    private List<WeakReference<Subscriber>> subscribers;
    private Object[] results;

    public void subscribe(Subscriber sub) {
        if (subscribers == null) {
            subscribers = new LinkedList<>();
        }
        synchronized (subscribers) {
            subscribers.add(new WeakReference<>(sub));
        }
    }

    public void unsubscribe() {
        subscribers = null;
    }

    public void unsubscribe(Subscriber sub) {
        synchronized (subscribers) {
            for (Iterator<WeakReference<Subscriber>> i = subscribers.iterator(); i.hasNext(); ) {
                WeakReference<Subscriber> subscriber = i.next();
                if (subscriber.get() == null || subscriber.get() == sub) {
                    i.remove();
                }
            }
        }
    }

    public void reset() {
        state = NON_INIT;
        results = null;
    }

    public boolean isPublished() {
        return state == PUBLISHED;
    }

    public void publish() {
        publish(true);
    }

    public void publish(boolean record, Object... results) {
        if (record)
            state = PUBLISHED;
        this.results = results;
        if (subscribers != null) {
            List<WeakReference<Subscriber>> subscribersCopy = new LinkedList<>();
            synchronized (subscribers) {
                subscribersCopy.addAll(subscribers);
            }
            for (WeakReference<Subscriber> subscriber : subscribersCopy) {
                if (subscriber.get() != null)
                    subscriber.get().onTopicPublished(this);
            }
        }
    }

    public Object[] getResults() {
        return results;
    }

    public boolean isPending() {
        return state == PENDING;
    }

    public void setPending() {
        state = PENDING;
    }

    public interface Subscriber {
        default void subscribeTopics() {
            for (Topic topic : getSubscription()) {
                if (topic.isPublished()) {
                    onTopicPublished(topic);
                }
                topic.subscribe(this);
            }
        }

        default void unsubscribeTopics() {
            for (Topic event : getSubscription()) {
                event.unsubscribe(this);
            }
        }

        void onTopicPublished(Topic topic);

        Topic[] getSubscription();
    }
}
