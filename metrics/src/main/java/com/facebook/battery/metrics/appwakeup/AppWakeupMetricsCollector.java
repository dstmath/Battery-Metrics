/**
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
 *
 * <p>This source code is licensed under the BSD-style license found in the LICENSE file in the root
 * directory of this source tree. An additional grant of patent rights can be found in the PATENTS
 * file in the same directory.
 */
package com.facebook.battery.metrics.appwakeup;

import static com.facebook.battery.metrics.appwakeup.AppWakeupMetrics.WakeupDetails;

import android.os.SystemClock;
import com.facebook.battery.metrics.api.SystemMetricsCollector;
import com.facebook.battery.metrics.api.SystemMetricsLogger;
import com.facebook.infer.annotation.ThreadSafe;

/**
 * Collects data about app wakeup counts, duration and the reasons. Used for attribution for alarms,
 * jobscheduler.
 */
@ThreadSafe
public class AppWakeupMetricsCollector extends SystemMetricsCollector<AppWakeupMetrics> {
  private static final String TAG = "AppWakeupMetricsCollector";

  private final AppWakeupMetrics mMetrics;
  private final AppWakeupMetrics mRunningWakeups;

  public AppWakeupMetricsCollector() {
    mMetrics = new AppWakeupMetrics();
    mRunningWakeups = new AppWakeupMetrics();
  }

  @Override
  @ThreadSafe(enableChecks = false)
  public synchronized boolean getSnapshot(AppWakeupMetrics snapshot) {
    if (snapshot == null) {
      throw new IllegalArgumentException("Null value passed to getSnapshot!");
    }
    // TODO: Optimize by taking intersection of the two lists
    snapshot.appWakeups.clear();
    for (int i = 0; i < mMetrics.appWakeups.size(); i++) {
      WakeupDetails details = new WakeupDetails();
      details.set(mMetrics.appWakeups.valueAt(i));
      snapshot.appWakeups.put(mMetrics.appWakeups.keyAt(i), details);
    }
    return true;
  }

  @Override
  public AppWakeupMetrics createMetrics() {
    return new AppWakeupMetrics();
  }

  public synchronized void recordWakeupStart(AppWakeupMetrics.WakeupReason reason, String id) {
    if (mRunningWakeups.appWakeups.containsKey(id)) {
      // This condition is possible depending on the usage of tags. We can see from this soft
      // error if this condition is indeed occurring in our codebase.
      SystemMetricsLogger.wtf(
          TAG, "Wakeup started again without ending for " + id + " (" + reason + ")");
      return;
    }
    mRunningWakeups.appWakeups.put(
        id, new AppWakeupMetrics.WakeupDetails(reason, 1, SystemClock.elapsedRealtime()));
  }

  public synchronized void recordWakeupEnd(String id) {
    if (!mRunningWakeups.appWakeups.containsKey(id)) {
      SystemMetricsLogger.wtf(TAG, "Wakeup stopped before starting for " + id);
      return;
    }
    AppWakeupMetrics.WakeupDetails details = mRunningWakeups.appWakeups.get(id);
    details.wakeupTimeMs = SystemClock.elapsedRealtime() - details.wakeupTimeMs;
    if (!mMetrics.appWakeups.containsKey(id)) {
      mMetrics.appWakeups.put(id, new AppWakeupMetrics.WakeupDetails().set(details));
    } else {
      mMetrics.appWakeups.get(id).sum(details, mMetrics.appWakeups.get(id));
    }
    mRunningWakeups.appWakeups.remove(id);
  }
}