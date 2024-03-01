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

public class DynamicLogLevels {

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(OpAmpClient.class.getName());

  public static final Logger ROOT_LOGGER = getLogger(Logger.ROOT_LOGGER_NAME);
  private final Map<String, Level> originalLevels = new HashMap<>();

  private Level originalRootLevel;

  void init() {
    originalRootLevel = ROOT_LOGGER.getLevel();

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
  }

  void applyLogLevels(OpAmpConfig.LogLevel[] levels) {
    for (OpAmpConfig.LogLevel logLevel : levels) {
      String name = logLevel.logger;

      Severity severity = Severity.valueOf(logLevel.level);

      Logger l = getLogger(name);
      if (l != null) {
        if (!originalLevels.containsKey(name)) {
          originalLevels.put(name, l.getLevel());
        }
        l.setLevel(severityToLevel(severity));
      } else {
        logger.info("Logger not found: " + name);
      }
    }
  }

  private static Logger getLogger(String logger1) {
    return (Logger) org.slf4j.LoggerFactory.getLogger(logger1);
  }

  private Level severityToLevel(Severity severity) {
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
    }
    throw new IllegalArgumentException("unsupported severity: " + severity);
  }
}
