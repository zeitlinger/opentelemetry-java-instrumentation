/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.http;

public final class HttpClientResponseConsumerHolder {
  private static volatile HttpClientResponseConsumer responseConsumer = new NoOpConsumer();

  public static void setConsumer(HttpClientResponseConsumer customizer) {
    HttpClientResponseConsumerHolder.responseConsumer = customizer;
  }

  public static HttpClientResponseConsumer getCustomizer() {
    return responseConsumer;
  }

  private HttpClientResponseConsumerHolder() {}

  private static class NoOpConsumer implements HttpClientResponseConsumer {

    @Override
    public <REQUEST, RESPONSE> void consume(
        HttpCommonAttributesGetter<REQUEST, RESPONSE> getter, REQUEST request, RESPONSE response) {}
  }
}
