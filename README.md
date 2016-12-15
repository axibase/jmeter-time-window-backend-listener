# Time Window Backend Listener
The alternative for standart Graphite Backend Listener that apply only metrics size sliding windows.
The listener send statistics about samplers that collected for all metrics in time window.


## Installation

1. Download jar file from release page
2. Copy the file to `$JMETER_HOME/lib/ext/` folder
3. Restart Jmeter if it is running

## Usage

1. Add Backend Listener to your Test plan
2. Select Backend Listener Implementation as `org.apache.jmeter.visualizers.backend.graphite.TimeWindowBackendListenerClient` 
3. Configure it.


## Configuration

### Properties

#### backend_time_window_size
Yet listener has only one property - size of the sliding window in seconds `backend_time_window_size`
You can specify it in `user.properties` file. Default value of the property is `15 second`.

```properties
backend_time_window_size=60#set window size to one minute
```

#### backend_time_window_start_count

Number of window after which backern listener will start to send statistic about windows.  
```properties
backend_time_window_start_count=5#set number of start window to send statistic
```



### Arguments

From jmeter UI or jmx file you can set following settings for listener

| Argument | Description | Default value |
| -------- | ----------- | ------------- |
| **graphiteMetricsSender**| Class that will send  statistics. | Standard graphite sender class |
| **graphiteHost**| Host of your graphite server | localhost |
| **graphitePort** | Port of your graphite server | 2003 |
| **rootMetricsPrefix** | Metrics prefix | jmeter. |
| **percentiles** | List of percentiles that should be sent separeted by `;` character | 90;95;99 |

