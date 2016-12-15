package org.apache.jmeter.visualizers.backend.graphite;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.apache.jmeter.visualizers.backend.BatchStatistic;
import org.apache.jmeter.visualizers.backend.Statistic;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class TimeWindowBackendListenerClient extends AbstractBackendListenerClient implements Runnable {
    //User properties
    private static final Integer WINDOW_SIZE = JMeterUtils.getPropDefault("backend_time_window_size", 15);
    private static final Integer START_COUNT = JMeterUtils.getPropDefault("backend_time_window_start_count", 4);
    //- Argument names
    private static final String GRAPHITE_METRICS_SENDER = "graphiteMetricsSender"; //$NON-NLS-1$
    private static final String GRAPHITE_HOST = "graphiteHost"; //$NON-NLS-1$
    private static final String GRAPHITE_PORT = "graphitePort"; //$NON-NLS-1$
    private static final String ROOT_METRICS_PREFIX = "rootMetricsPrefix"; //$NON-NLS-1$
    private static final String PERCENTILES = "percentiles"; //$NON-NLS-1$
    //Argument values
    private static final int DEFAULT_PLAINTEXT_PROTOCOL_PORT = 2003;
    private static final String ALL_CONTEXT_NAME = "all";
    private static final Logger LOGGER = LoggingManager.getLoggerForClass();
    private static final String DEFAULT_METRICS_PREFIX = "jmeter."; //$NON-NLS-1$
    private static final String DEFAULT_PERCENTILES = "90;95;99";

    private static final int MAX_POOL_SIZE = 1;
    private static final String SEPARATOR = ";";
    private Integer count = 0;
    private List<SampleResult> batch = new ArrayList<>();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timerHandle;
    private GraphiteMetricsSender graphiteMetricsManager;
    private String rootMetricsPrefix;
    private Set<Double> percentilies;
    private int graphitePort;
    private String graphiteHost;

    public TimeWindowBackendListenerClient() {
        count = 0;
    }

    private void sendMetrics() {
        count++;
        try {
            if (count >= START_COUNT) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Sending statistics about %d results", batch.size()));
                }
                Statistic batchStatistic = BatchStatistic.getStatistic(batch);
                addStatisticToSend(batchStatistic);
                graphiteMetricsManager.writeAndSendMetrics();
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage());
        } finally {
            batch.clear();
        }
    }

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
        batch.addAll(sampleResults);
    }

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        graphiteHost = context.getParameter(GRAPHITE_HOST);
        graphitePort = context.getIntParameter(GRAPHITE_PORT, DEFAULT_PLAINTEXT_PROTOCOL_PORT);
        rootMetricsPrefix = context.getParameter(ROOT_METRICS_PREFIX, DEFAULT_METRICS_PREFIX);
        String graphiteMetricsSenderClass = context.getParameter(GRAPHITE_METRICS_SENDER);
        Class<?> clazz = Class.forName(graphiteMetricsSenderClass);
        this.graphiteMetricsManager = (GraphiteMetricsSender) clazz.newInstance();
        graphiteMetricsManager.setup(graphiteHost, graphitePort, rootMetricsPrefix);
        rootMetricsPrefix = context.getParameter(ROOT_METRICS_PREFIX, DEFAULT_METRICS_PREFIX);
        String[] percentilesStringArray = context.getParameter(PERCENTILES, DEFAULT_METRICS_PREFIX).split(SEPARATOR);
        percentilies = new HashSet<>();
        for (String percentilesString : percentilesStringArray) {
            if (!StringUtils.isEmpty(percentilesString.trim())) {
                try {
                    Double percentileValue = Double.valueOf(percentilesString.trim());
                    if (percentileValue >= 0 && percentileValue < 100) {
                        percentilies.add(percentileValue);
                    } else {
                        LOGGER.error(String.format("Incorrect value %f for percentile", percentileValue));
                    }
                } catch (Exception e) {
                    LOGGER.error("Error parsing percentile:'" + percentilesString + "'", e);
                }
            }
        }

        scheduler = Executors.newScheduledThreadPool(MAX_POOL_SIZE);
        this.timerHandle = scheduler.scheduleAtFixedRate(this, WINDOW_SIZE, WINDOW_SIZE, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        sendMetrics();
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        timerHandle.cancel(false);
        scheduler.shutdown();
        graphiteMetricsManager.destroy();
        super.teardownTest(context);
    }

    private void addStatisticToSend(Statistic statistic) {
        long time = TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        if (statistic.getTotal() > 0) {
            graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "all.count", String.valueOf(statistic.getTotal()));
            graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "all.min", String.valueOf(statistic.getAllMinTime()));
            graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "all.max", String.valueOf(statistic.getAllMaxTime()));
            graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "all.mean", String.valueOf(statistic.getAllMean()));
            if (statistic.getSuccesses() > 0) {
                graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "ok.count", String.valueOf(statistic.getSuccesses()));
                graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "ok.min", String.valueOf(statistic.getOkMinTime()));
                graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "ok.max", String.valueOf(statistic.getOkMaxTime()));
                graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "ok.mean", String.valueOf(statistic.getOkMean()));
            }
            if (statistic.getFailures() > 0) {
                graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "fail.count", String.valueOf(statistic.getFailures()));
                graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "fail.min", String.valueOf(statistic.getKoMinTime()));
                graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "fail.max", String.valueOf(statistic.getKoMaxTime()));
                graphiteMetricsManager.addMetric(time, ALL_CONTEXT_NAME, "fail.mean", String.valueOf(statistic.getKoMean()));
            }
        }

        for (Double value : percentilies) {
            DecimalFormat df = new DecimalFormat("###.#");
            if (statistic.getTotal() > 0) {
                graphiteMetricsManager.addMetric(time,
                        ALL_CONTEXT_NAME,
                        String.format("all.pct%s", df.format(value)),
                        String.valueOf(statistic.getAllPercentile(value))
                );
            }
            if (statistic.getSuccesses() > 0) {
                graphiteMetricsManager.addMetric(time,
                        ALL_CONTEXT_NAME,
                        String.format("ok.pct%s", df.format(value)),
                        String.valueOf(statistic.getOkPercentile(value))
                );
            }
            if (statistic.getFailures() > 0) {
                graphiteMetricsManager.addMetric(time,
                        ALL_CONTEXT_NAME,
                        String.format("fail.pct%s", df.format(value)),
                        String.valueOf(statistic.getKoPercentile(value))
                );
            }
        }
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument(GRAPHITE_METRICS_SENDER, TextGraphiteMetricsSender.class.getName());
        arguments.addArgument(GRAPHITE_HOST, "");
        arguments.addArgument(GRAPHITE_PORT, Integer.toString(DEFAULT_PLAINTEXT_PROTOCOL_PORT));
        arguments.addArgument(ROOT_METRICS_PREFIX, DEFAULT_METRICS_PREFIX);
        arguments.addArgument(PERCENTILES, DEFAULT_PERCENTILES);
        return arguments;
    }
}
