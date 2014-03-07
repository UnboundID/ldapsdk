/*
 * Copyright 2009-2014 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2014 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.util;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static com.unboundid.util.Debug.*;



/**
 * Instances of this class are used to ensure that certain actions are performed
 * at a fixed rate per interval (e.g. 10000 search operations per second).
 * <p>
 * Once a class is constructed with the duration of an interval and the target
 * per interval, the {@link #await} method only releases callers at the
 * specified number of times per interval.  This class is most useful when
 * the target number per interval exceeds the limits of other approaches
 * such as {@code java.util.Timer} or
 * {@code java.util.concurrent.ScheduledThreadPoolExecutor}.  For instance,
 * this does a good job of ensuring that something happens about 10000 times
 * per second, but it's overkill to ensure something happens five times per
 * hour.  This does come at a cost.  In the worst case, a single thread is
 * tied up in a loop doing a small amount of computation followed by a
 * Thread.yield().  Calling Thread.sleep() is not possible because many
 * platforms sleep for a minimum of 10ms, and all platforms require sleeping
 * for at least 1ms.
 * <p>
 * Testing has shown that this class is accurate for a "no-op"
 * action up to two million per second, which vastly exceeds its
 * typical use in tools such as {@code searchrate} and {@code modrate}.  This
 * class is designed to be called by multiple threads, however, it does not
 * make any fairness guarantee between threads; a single-thread might be
 * released from the {@link #await} method many times before another thread
 * that is blocked in that method.
 * <p>
 * This class attempts to smooth out the target per interval throughout each
 * interval.  At a given ratio, R between 0 and 1, through the interval, the
 * expected number of actions to have been performed in the interval at that
 * time is R times the target per interval.  That is, 10% of the way through
 * the interval, approximately 10% of the actions have been performed, and
 * 80% of the way through the interval, 80% of the actions have been performed.
 */
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class FixedRateBarrier
       implements Serializable
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -3490156685189909611L;

  /**
   * The minimum number of milliseconds that Thread.sleep() can handle
   * accurately.  This varies from platform to platform, so we measure it
   * once in the static initializer below.  When using a low rate (such as
   * 100 per second), we can often sleep between iterations instead of having
   * to spin calling Thread.yield().
   */
  private static final long minSleepMillis;

  static
  {
    // Calibrate the minimum number of milliseconds that we can reliably
    // sleep on this system.  We take several measurements and take the median,
    // which keeps us from choosing an outlier.
    //
    // It varies from system to system.  Testing on three systems, yielded
    // three different measurements Solaris x86 (10 ms), RedHat Linux (2 ms),
    // Windows 7 (1 ms).

    final List<Long> minSleepMillisMeasurements = new ArrayList<Long>();

    for (int i = 0; i < 11; i++)
    {
      final long timeBefore = System.currentTimeMillis();
      try
      {
        Thread.sleep(1);
      }
      catch (InterruptedException e)
      {
        debugException(e);
      }
      final long sleepMillis = System.currentTimeMillis() - timeBefore;
      minSleepMillisMeasurements.add(sleepMillis);
    }

    Collections.sort(minSleepMillisMeasurements);
    final long medianSleepMillis = minSleepMillisMeasurements.get(
            minSleepMillisMeasurements.size()/2);

    minSleepMillis = Math.max(medianSleepMillis, 1);

    final String message = "Calibrated FixedRateBarrier to use " +
          "minSleepMillis=" + minSleepMillis + ".  " +
          "Minimum sleep measurements = " + minSleepMillisMeasurements;
    debug(Level.INFO, DebugType.OTHER, message);
  }


  // This tracks when this class is shut down.  Calls to await() after
  // shutdownRequested() is called, will return immediately with a value of
  // true.
  private volatile boolean shutdownRequested = false;


  //
  // The following class variables are guarded by synchronized(this).
  //

  // The duration of the target interval in nano-seconds.
  private long intervalDurationNanos;

  // This tracks the number of milliseconds between each iteration if they
  // were evenly spaced.
  //
  // If intervalDurationMs=1000 and perInterval=100, then this is 100.
  // If intervalDurationMs=1000 and perInterval=10000, then this is .1.
  private double millisBetweenIterations;

  // The target number of times to release a thread per interval.
  private int perInterval;

  // A count of the number of times that await has returned within the current
  // interval.
  private long countInThisInterval;

  // The start of this interval in terms of System.nanoTime().
  private long intervalStartNanos;

  // The end of this interval in terms of System.nanoTime().
  private long intervalEndNanos;



  /**
   * Constructs a new FixedRateBarrier, which is active until
   * {@link #shutdownRequested} is called.
   *
   * @param  intervalDurationMs  The duration of the interval in milliseconds.
   * @param  perInterval  The target number of times that {@link #await} should
   *                      return per interval.
   */
  public FixedRateBarrier(final long intervalDurationMs, final int perInterval)
  {
    setRate(intervalDurationMs, perInterval);
  }



  /**
   * Updates the rates associated with this FixedRateBarrier.  The new rate
   * will be in effect when this method returns.
   *
   * @param  intervalDurationMs  The duration of the interval in milliseconds.
   * @param  perInterval  The target number of times that {@link #await} should
   *                      return per interval.
   */
  public synchronized void setRate(final long intervalDurationMs,
                                   final int perInterval)
  {
    Validator.ensureTrue(intervalDurationMs > 0,
         "FixedRateBarrier.intervalDurationMs must be at least 1.");
    Validator.ensureTrue(perInterval > 0,
         "FixedRateBarrier.perInterval must be at least 1.");

    this.perInterval = perInterval;

    intervalDurationNanos = 1000L * 1000L * intervalDurationMs;

    millisBetweenIterations = (double)intervalDurationMs/(double)perInterval;

    // Reset the intervals and all of the counters.
    countInThisInterval = 0;
    intervalStartNanos = 0;
    intervalEndNanos = 0;
  }



  /**
   * This method waits until it is time for the next 'action' to be performed
   * based on the specified interval duration and target per interval.  This
   * method can be called by multiple threads simultaneously.  This method
   * returns immediately if shutdown has been requested.
   *
   * @return  {@code true} if shutdown has been requested and {@code} false
   *          otherwise.
   */
  public synchronized boolean await()
  {
    // Loop forever until we are requested to shutdown or it is time to perform
    // the next 'action' in which case we break from the loop.
    while (!shutdownRequested)
    {
      final long now = System.nanoTime();

      if ((intervalStartNanos == 0) ||   // Handles the first time we're called.
          (now < intervalStartNanos))    // Handles a change in the clock.
      {
        intervalStartNanos = now;
        intervalEndNanos = intervalStartNanos + intervalDurationNanos;
      }
      else if (now >= intervalEndNanos)  // End of an interval.
      {
        countInThisInterval = 0;

        if (now < (intervalEndNanos + intervalDurationNanos))
        {
          // If we have already passed the end of the next interval, then we
          // don't try to catch up.  Instead we just reset the start of the
          // next interval to now.  This could happen if the system clock
          // was set to the future, we're running in a debugger, or we have
          // very short intervals and are unable to keep up.
          intervalStartNanos = now;
        }
        else
        {
          // Usually we're some small fraction into the next interval, so
          // we set the start of the current interval to the end of the
          // previous one.
          intervalStartNanos = intervalEndNanos;
        }
        intervalEndNanos = intervalStartNanos + intervalDurationNanos;
      }

      final long intervalRemaining = intervalEndNanos - now;
      if (intervalRemaining <= 0)
      {
        // This shouldn't happen, but we're careful not to divide by 0.
        continue;
      }

      final double intervalFractionRemaining =
           (double) intervalRemaining / intervalDurationNanos;

      final double expectedRemaining = intervalFractionRemaining * perInterval;
      final long actualRemaining = perInterval - countInThisInterval;

      if (actualRemaining >= expectedRemaining)
      {
        // We are on schedule or behind schedule so let the next 'action'
        // happen.
        countInThisInterval++;
        break;
      }
      else
      {
        // If we can sleep until it's time to leave this barrier, then do
        // so to keep from spinning on a CPU doing Thread.yield().

        final double gapIterations = expectedRemaining - actualRemaining;
        final long remainingMillis =
             (long) Math.floor(millisBetweenIterations * gapIterations);

        if (remainingMillis >= minSleepMillis)
        {
          // Cap how long we sleep so that we can respond to a change in the
          // rate without too much delay.
          final long waitTime = Math.min(remainingMillis, 10);
          try
          {
            // We need to wait here instead of Thread.sleep so that we don't
            // block setRate.
            this.wait(waitTime);
          }
          catch (InterruptedException e)
          {
            debugException(e);
          }
        }
        else
        {
          // We're ahead of schedule so yield to other threads, and then try
          // again.  Note: this is the most costly part of the algorithm because
          // we have to busy wait due to the lack of sleeping for very small
          // amounts of time.
          Thread.yield();
        }
      }
    }

    return shutdownRequested;
  }



  /**
   * Retrieves information about the current target rate for this barrier.  The
   * value returned will include a {@code Long} that specifies the duration of
   * the current interval in milliseconds and an {@code Integer} that specifies
   * the number of times that the {@link #await} method should return per
   * interval.
   *
   * @return  Information about hte current target rate for this barrier.
   */
  public synchronized ObjectPair<Long,Integer> getTargetRate()
  {
    return new ObjectPair<Long,Integer>(
         (intervalDurationNanos / (1000L * 1000L)),
         perInterval);
  }



  /**
   * Shuts down this barrier.  Future calls to await() will return immediately.
   */
  public void shutdownRequested()
  {
    shutdownRequested = true;
  }



  /**
   * Returns {@code true} if shutdown has been requested.
   *
   * @return  {@code true} if shutdown has been requested and {@code false}
   *          otherwise.
   */
  public boolean isShutdownRequested()
  {
    return shutdownRequested;
  }
}
