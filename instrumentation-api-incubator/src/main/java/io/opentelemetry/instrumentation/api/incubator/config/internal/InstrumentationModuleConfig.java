/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.config.internal;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class InstrumentationModuleConfig {
  private InstrumentationModuleConfig() {}

  public static boolean isInstrumentationEnabled(
      InstrumentationConfig config, Iterable<String> instrumentationNames, boolean defaultEnabled) {
    // If default is enabled, we want to disable individually,
    // if default is disabled, we want to enable individually.
    boolean anyEnabled = defaultEnabled;
    for (String name : instrumentationNames) {
      String propertyName = "otel.instrumentation." + name + ".enabled";
      boolean enabled = config.getBoolean(propertyName, defaultEnabled);

      if (defaultEnabled) {
        anyEnabled &= enabled;
      } else {
        anyEnabled |= enabled;
      }
    }
    return anyEnabled;
  }
}
