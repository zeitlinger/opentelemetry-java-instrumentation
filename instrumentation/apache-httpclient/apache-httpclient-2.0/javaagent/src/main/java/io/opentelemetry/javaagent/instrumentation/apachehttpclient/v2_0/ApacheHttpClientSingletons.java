/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v2_0;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.javaagent.bootstrap.internal.HttpClientInstrumenterFactory;
import org.apache.commons.httpclient.HttpMethod;

public final class ApacheHttpClientSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.apache-httpclient-2.0";

  private static final Instrumenter<HttpMethod, HttpMethod> INSTRUMENTER;

  static {
    INSTRUMENTER =
        HttpClientInstrumenterFactory.builder(
                INSTRUMENTATION_NAME, new ApacheHttpClientHttpAttributesGetter())
            .buildClientInstrumenter(HttpHeaderSetter.INSTANCE);
  }

  public static Instrumenter<HttpMethod, HttpMethod> instrumenter() {
    return INSTRUMENTER;
  }

  private ApacheHttpClientSingletons() {}
}
