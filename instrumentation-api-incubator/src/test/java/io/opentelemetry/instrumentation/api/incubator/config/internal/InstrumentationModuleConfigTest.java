/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.config.internal;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class InstrumentationModuleConfigTest {
  @ParameterizedTest(name = "isInstrumentationEnabled({0}) = {4}")
  @ArgumentsSource(InstrumentationEnabledParams.class)
  void testIsInstrumentationEnabled(
      @SuppressWarnings("unused") String description,
      boolean firstEnabled,
      boolean secondEnabled,
      boolean defaultEnabled,
      boolean expected) {

    InstrumentationConfig config = mock(InstrumentationConfig.class);
    when(config.getBoolean("otel.instrumentation.first.enabled", defaultEnabled))
        .thenReturn(firstEnabled);
    when(config.getBoolean("otel.instrumentation.second.enabled", defaultEnabled))
        .thenReturn(secondEnabled);

    assertEquals(
        expected,
        InstrumentationModuleConfig.isInstrumentationEnabled(
            config, new TreeSet<>(asList("first", "second")), defaultEnabled));
  }

  private static class InstrumentationEnabledParams implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of(
              "enabled by default, both instrumentations are off", false, false, true, false),
          Arguments.of("enabled by default, one instrumentation is on", true, false, true, false),
          Arguments.of("enabled by default, both instrumentations are on", true, true, true, true),
          Arguments.of(
              "disabled by default, both instrumentations are off", false, false, false, false),
          Arguments.of("disabled by default, one instrumentation is on", true, false, false, true),
          Arguments.of(
              "disabled by default, both instrumentation are on", true, true, false, true));
    }
  }
}
