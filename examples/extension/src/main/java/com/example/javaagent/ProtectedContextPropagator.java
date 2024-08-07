/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.javaagent;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.ArrayList;
import java.util.List;

public class ProtectedContextPropagator implements TextMapPropagator {
  private static final List<String> FIELDS = new ArrayList<>();

  private static final String FIELD = "X-protected-propagation";

  static {
    FIELDS.addAll(W3CBaggagePropagator.getInstance().fields());
    FIELDS.add(FIELD);
  }

  private final TextMapPropagator propagator;

  public ProtectedContextPropagator(TextMapPropagator propagator) {
    this.propagator = propagator;
  }

  @Override
  public List<String> fields() {
    return FIELDS;
  }

  @Override
  public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
    propagator.inject(context, carrier, setter);
    if (context != null && setter != null && Span.fromContext(context).getSpanContext().isValid()) {
      setter.set(carrier, FIELD, "true");
    }
  }

  @Override
  public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
    String enabled = getter.get(carrier, FIELD);
    if (enabled != null) {
      return propagator.extract(context, carrier, getter);
    } else {
      return context;
    }
  }
}
