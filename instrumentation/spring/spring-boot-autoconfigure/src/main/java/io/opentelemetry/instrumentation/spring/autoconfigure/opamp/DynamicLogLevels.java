/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.opamp;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import io.opentelemetry.api.logs.Severity;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class DynamicLogLevels {

  public static class LogDecision {

    private final Severity severity;
    private final Severity samplingSeverity;

    public LogDecision(Severity severity, Severity samplingSeverity) {
      this.severity = severity;
      this.samplingSeverity = samplingSeverity;
    }

    public Severity getSeverity() {
      return severity;
    }

    public Severity getSamplingSeverity() {
      return samplingSeverity;
    }
  }

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(OpAmpClient.class.getName());

  public static final Logger ROOT_LOGGER = getLogger(Logger.ROOT_LOGGER_NAME);
  private final Map<String, Level> originalLevels;
  private Map<String, Severity> otelLevels = new HashMap<>();
  private Map<String, Severity> samplingOtelLevels = new HashMap<>();

  private final Map<String, Severity> availableLoggers;

  public DynamicLogLevels(
      Map<String, Severity> availableLoggers, Map<String, Level> originalLevels) {
    this.availableLoggers = availableLoggers;
    this.originalLevels = originalLevels;
  }

  public static DynamicLogLevels create() {
    Map<String, Level> originalLevels = new ConcurrentHashMap<>();

    Level originalRootLevel = ROOT_LOGGER.getLevel();

    ROOT_LOGGER
        .iteratorForAppenders()
        .forEachRemaining(
            appender -> {
              if (!(appender
                  instanceof
                  io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender)) {
                appender.addFilter(
                    new Filter<ILoggingEvent>() {
                      @Override
                      public FilterReply decide(ILoggingEvent event) {
                        String loggerName = event.getLoggerName();
                        Level level = originalLevels.get(loggerName);
                        if (level == null) {
                          level = originalRootLevel;
                        }

                        if (event.getLevel().isGreaterOrEqual(level)) {
                          return FilterReply.NEUTRAL;
                        } else {
                          return FilterReply.DENY;
                        }
                      }
                    });
              }
            });

    return new DynamicLogLevels(readAvailableLoggers(), originalLevels);
  }

  void applyLogLevels(OpAmpConfig config) {
    this.otelLevels =
        applyLevels(
            config.logLevels,
            (l, severity) -> {
              String name = l.getName();
              if (!originalLevels.containsKey(name)) {
                originalLevels.put(name, l.getEffectiveLevel());
              }
              Level original = originalLevels.get(name);
              Level otel = severityToLevel(severity);
              Level effective = original.toInt() < otel.toInt() ? original : otel;
              l.setLevel(effective);
            });
    this.samplingOtelLevels = applyLevels(config.samplingLogLevels, (l, severity) -> {});
  }

  private static Map<String, Severity> applyLevels(
      OpAmpConfig.LogLevel[] levels, BiConsumer<Logger, Severity> applyLevel) {
    Map<String, Severity> result = new HashMap<>();

    if (levels == null) {
      return result;
    }
    for (OpAmpConfig.LogLevel logLevel : levels) {
      String name = logLevel.logger;

      Severity severity = Severity.valueOf(logLevel.level);

      Logger l = getLogger(name);
      if (l != null) {
        applyLevel.accept(l, severity);
        result.put(name, severity);
      } else {
        logger.info("Logger not found: " + name);
      }
    }
    return result;
  }

  public LogDecision getLogDecision(String loggerName) {
    return new LogDecision(
        getEffectiveLevel(loggerName, otelLevels),
        getEffectiveLevel(loggerName, samplingOtelLevels));
  }

  static Severity getEffectiveLevel(String loggerName, Map<String, Severity> levels) {
    // find the most specific severity
    while (loggerName != null) {
      Severity severity = levels.get(loggerName);
      if (severity != null) {
        return severity;
      }
      int i = loggerName.lastIndexOf('.');
      if (i == -1) {
        break;
      }
      loggerName = loggerName.substring(0, i);
    }
    return null;
  }

  private static Logger getLogger(String name) {
    return (Logger) org.slf4j.LoggerFactory.getLogger(name);
  }

  private static Level severityToLevel(Severity severity) {
    switch (severity) {
      case TRACE:
        return Level.TRACE;
      case DEBUG:
        return Level.DEBUG;
      case INFO:
        return Level.INFO;
      case WARN:
        return Level.WARN;
      case ERROR:
        return Level.ERROR;
      default:
        break;
    }
    throw new IllegalArgumentException("unsupported severity: " + severity);
  }

  private static Severity levelToSeverity(Level level) {
    switch (level.levelInt) {
      case Level.TRACE_INT:
        return Severity.TRACE;
      case Level.DEBUG_INT:
        return Severity.DEBUG;
      case Level.INFO_INT:
        return Severity.INFO;
      case Level.WARN_INT:
        return Severity.WARN;
      case Level.ERROR_INT:
        return Severity.ERROR;
      default:
        break;
    }
    throw new IllegalArgumentException("unsupported level: " + level);
  }

  static Map<String, Severity> readAvailableLoggers() {
    return ROOT_LOGGER.getLoggerContext().getLoggerList().stream()
        .collect(Collectors.toMap(Logger::getName, l -> levelToSeverity(l.getEffectiveLevel())));
  }

  public Map<String, Severity> getAvailableLoggers() {
    return availableLoggers;
  }
}
