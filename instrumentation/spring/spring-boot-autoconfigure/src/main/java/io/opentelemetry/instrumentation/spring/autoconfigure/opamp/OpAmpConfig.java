/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.opamp;

import io.opentelemetry.api.logs.Severity;
import java.util.Map;

public class OpAmpConfig {
  public static class LogLevel {
    public String logger;
    public String level;
  }

  public LogLevel[] logLevels;
  public LogLevel[] samplingLogLevels;

  public Map<String, Severity> availableLoggers;

  public double sampleRatio;
}
