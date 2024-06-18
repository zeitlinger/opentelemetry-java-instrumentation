/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jetty.httpclient.v9_2.internal;

import io.opentelemetry.context.propagation.TextMapSetter;
import org.eclipse.jetty.client.api.Request;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public enum HttpHeaderSetter implements TextMapSetter<Request> {
  INSTANCE;

  @Override
  public void set(Request request, String key, String value) {
    if (request != null) {
      // dedupe header fields here with a put()
      request.getHeaders().put(key, value);
    }
  }
}
