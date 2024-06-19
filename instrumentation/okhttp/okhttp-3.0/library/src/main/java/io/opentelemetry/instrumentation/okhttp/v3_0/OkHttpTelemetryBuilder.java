/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.okhttp.v3_0;

import static io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor.alwaysClient;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.AbstractHttpClientTelemetryBuilder;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpAttributesGetter;
import okhttp3.Request;
import okhttp3.Response;

/** A builder of {@link OkHttpTelemetry}. */
public final class OkHttpTelemetryBuilder
    extends AbstractHttpClientTelemetryBuilder<OkHttpTelemetryBuilder, Request, Response> {

  public static final String INSTRUMENTATION_NAME = "io.opentelemetry.okhttp-3.0";

  OkHttpTelemetryBuilder(OpenTelemetry openTelemetry) {
    super(INSTRUMENTATION_NAME, openTelemetry, OkHttpAttributesGetter.INSTANCE);
  }

  /**
   * Returns a new {@link OkHttpTelemetry} with the settings of this {@link OkHttpTelemetryBuilder}.
   */
  public OkHttpTelemetry build() {
    return new OkHttpTelemetry(
        instrumenterBuilder().buildInstrumenter(alwaysClient()), openTelemetry.getPropagators());
  }
}
