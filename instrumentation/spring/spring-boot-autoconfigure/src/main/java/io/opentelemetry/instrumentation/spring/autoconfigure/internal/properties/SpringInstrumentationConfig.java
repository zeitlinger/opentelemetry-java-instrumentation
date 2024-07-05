/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.internal.properties;

import io.opentelemetry.instrumentation.api.incubator.config.internal.InstrumentationConfig;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class SpringInstrumentationConfig implements InstrumentationConfig {

  private final Environment environment;

  public SpringInstrumentationConfig(Environment environment) {
    this.environment = environment;
  }

  @Override
  public String getString(String name) {
    return environment.getProperty(name);
  }

  @Override
  public String getString(String name, String defaultValue) {
    return environment.getProperty(name, defaultValue);
  }

  @Override
  public boolean getBoolean(String name, boolean defaultValue) {
    return environment.getProperty(name, Boolean.class, defaultValue);
  }

  @Override
  public int getInt(String name, int defaultValue) {
    return environment.getProperty(name, Integer.class, defaultValue);
  }

  @Override
  public long getLong(String name, long defaultValue) {
    return environment.getProperty(name, Long.class, defaultValue);
  }

  @Override
  public double getDouble(String name, double defaultValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Duration getDuration(String name, Duration defaultValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> getList(String name, List<String> defaultValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, String> getMap(String name, Map<String, String> defaultValue) {
    throw new UnsupportedOperationException();
  }
}
