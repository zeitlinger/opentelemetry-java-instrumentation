/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.http;

public interface HttpClientResponseConsumer {
  <REQUEST, RESPONSE> void consume(
      HttpCommonAttributesGetter<REQUEST, RESPONSE> getter, REQUEST request, RESPONSE response);
}
