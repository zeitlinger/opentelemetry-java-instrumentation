/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.javaagent;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

@AutoService(ConfigurablePropagatorProvider.class)
public class ProtectedContextPropagatorProvider implements ConfigurablePropagatorProvider {
  @Override
  public TextMapPropagator getPropagator(ConfigProperties config) {
    return new ProtectedBaggagePropagator(W3CTraceContextPropagator.getInstance());
  }

  @Override
  public String getName() {
    return "protected-context";
  }
}
