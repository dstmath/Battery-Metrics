// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.battery.metrics.network;

import static com.facebook.battery.metrics.network.NetworkBytesCollector.BG;
import static com.facebook.battery.metrics.network.NetworkBytesCollector.FG;

import android.content.Context;
import com.facebook.battery.metrics.core.SystemMetricsCollector;
import com.facebook.infer.annotation.ThreadSafe;

/**
 * Alternative to {@link NetworkMetricsCollector} which supports distinguishing
 * foreground/background app states where possible (in particular when using qtaguid stats).
 */
public class EnhancedNetworkMetricsCollector
    extends SystemMetricsCollector<EnhancedNetworkMetrics> {

  private boolean mIsValid = true;
  private final NetworkBytesCollector mCollector;
  private final long[] mBytes;
  private final long[] mPrevBytes;

  public EnhancedNetworkMetricsCollector(Context context) {
    mCollector = NetworkBytesCollector.create(context);
    mBytes = NetworkBytesCollector.createByteArray();
    mPrevBytes = NetworkBytesCollector.createByteArray();
  }

  @Override
  @ThreadSafe(enableChecks = false)
  public synchronized boolean getSnapshot(EnhancedNetworkMetrics snapshot) {
    if (!mIsValid || !mCollector.getTotalBytes(mBytes)) {
      return false;
    }

    mIsValid = NetworkMetricsCollector.ensureBytesIncreased(mBytes, mPrevBytes);
    if (!mIsValid) {
      return false;
    }

    boolean supportsBgDetection = mCollector.supportsBgDistinction();
    snapshot.supportsBgDetection = supportsBgDetection;
    NetworkMetricsCollector.resetMetrics(snapshot.fgMetrics);
    NetworkMetricsCollector.addMetricsFromBytes(snapshot.fgMetrics, mBytes, FG);
    if (supportsBgDetection) {
      NetworkMetricsCollector.resetMetrics(snapshot.bgMetrics);
      NetworkMetricsCollector.addMetricsFromBytes(snapshot.bgMetrics, mBytes, BG);
    }

    return true;
  }

  @Override
  public EnhancedNetworkMetrics createMetrics() {
    return new EnhancedNetworkMetrics();
  }
}
