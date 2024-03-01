/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.opamp;

public class OpAmpConfig {
  public static class LogLevel {
    public String logger;
    public String level;
  }

  public LogLevel[] logLevels;

  public double sampleRatio;
}
