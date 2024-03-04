/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.opamp;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.logs.Severity;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DynamicLogLevelsTest {
  @ParameterizedTest
  @CsvSource({"a.b.c.D, ERROR", "a.b.c, ERROR", "a.b, ERROR", "a, ERROR", "x.y.z, null"})
  void getEffectiveLevel(String logger, String expected) {
    Map<String, Severity> levels = new HashMap<>();
    levels.put(logger, Severity.ERROR);

    Severity severity = DynamicLogLevels.getEffectiveLevel("a.b.c.D", levels);
    assertThat(String.valueOf(severity)).isEqualTo(expected);
  }
}
