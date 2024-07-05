/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.internal;

import io.opentelemetry.instrumentation.api.incubator.config.internal.InstrumentationModuleConfig;
import io.opentelemetry.instrumentation.spring.autoconfigure.internal.properties.SpringInstrumentationConfig;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class InstrumentationPropertyEnabled implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    Map<String, Object> attributes =
        Objects.requireNonNull(
            metadata.getAnnotationAttributes(ConditionalOnEnabledInstrumentation.class.getName()));

    Environment environment = context.getEnvironment();
    return InstrumentationModuleConfig.isInstrumentationEnabled(
        new SpringInstrumentationConfig(environment),
        Collections.singleton((String) attributes.get("module")),
        isEnabledByDefault(attributes, environment));
  }

  private static boolean isEnabledByDefault(
      Map<String, Object> attributes, Environment environment) {
    return (boolean) attributes.get("enabledByDefault")
        && environment.getProperty(
            "otel.instrumentation.common.default-enabled", Boolean.class, true);
  }
}
