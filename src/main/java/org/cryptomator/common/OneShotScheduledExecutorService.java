package org.cryptomator.common;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Marker interface to indicate the used {@link ScheduledExecutorService} does not support repeated execution via {@link java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}} or {@link java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}.
 * Calling one of those methods must throw an {@link UnsupportedOperationException}.
 */
public interface OneShotScheduledExecutorService extends ScheduledExecutorService {

}
