package org.apache.jmeter.visualizers.backend;


public interface Statistic {
    /**
     * Get the number of total requests for the current time slot
     *
     * @return number of total requests
     */
    int getTotal();

    /**
     * Get the number of successful requests for the current time slot
     *
     * @return number of successful requests
     */
    int getSuccesses();

    /**
     * Get the number of failed requests for the current time slot
     *
     * @return number of failed requests
     */
    int getFailures();

    /**
     * Get the maximal elapsed time for requests within sliding window
     *
     * @return the maximal elapsed time, or <code>0</code> if no requests have
     * been added yet
     */
    double getOkMaxTime();

    /**
     * Get the minimal elapsed time for requests within sliding window
     *
     * @return the minTime, or {@link Long#MAX_VALUE} if no requests have been
     * added yet
     */
    double getOkMinTime();

    /**
     * Get the arithmetic mean of the stored values
     *
     * @return The arithmetic mean of the stored values
     */
    double getOkMean();

    /**
     * Returns an estimate for the requested percentile of the stored values.
     *
     * @param percentile the requested percentile (scaled from 0 - 100)
     * @return Returns an estimate for the requested percentile of the stored
     * values.
     */
    double getOkPercentile(double percentile);

    /**
     * Get the maximal elapsed time for requests within sliding window
     *
     * @return the maximal elapsed time, or <code>0</code> if no requests have
     * been added yet
     */
    double getKoMaxTime();

    /**
     * Get the minimal elapsed time for requests within sliding window
     *
     * @return the minTime, or {@link Long#MAX_VALUE} if no requests have been
     * added yet
     */
    double getKoMinTime();

    /**
     * Get the arithmetic mean of the stored values
     *
     * @return The arithmetic mean of the stored values
     */
    double getKoMean();

    /**
     * Returns an estimate for the requested percentile of the stored values.
     *
     * @param percentile the requested percentile (scaled from 0 - 100)
     * @return Returns an estimate for the requested percentile of the stored
     * values.
     */
    double getKoPercentile(double percentile);

    /**
     * Get the maximal elapsed time for requests within sliding window
     *
     * @return the maximal elapsed time, or <code>0</code> if no requests have
     * been added yet
     */
    double getAllMaxTime();

    /**
     * Get the minimal elapsed time for requests within sliding window
     *
     * @return the minTime, or {@link Long#MAX_VALUE} if no requests have been
     * added yet
     */
    double getAllMinTime();

    /**
     * Get the arithmetic mean of the stored values
     *
     * @return The arithmetic mean of the stored values
     */
    double getAllMean();

    /**
     * Returns an estimate for the requested percentile of the stored values.
     *
     * @param percentile the requested percentile (scaled from 0 - 100)
     * @return Returns an estimate for the requested percentile of the stored
     * values.
     */
    double getAllPercentile(double percentile);
}
