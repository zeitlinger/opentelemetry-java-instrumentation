/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.internal.properties;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import io.opentelemetry.sdk.autoconfigure.spi.internal.StructuredConfigProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class SpringConfigYamlProperties implements StructuredConfigProperties {
  private final ConfigurableEnvironment environment;
  private final String prefix;
  private final StructuredConfigProperties fallback;
  private final OtelSpringProperties otelSdkProperties;

  public SpringConfigYamlProperties(
      ConfigurableEnvironment environment, String prefix, StructuredConfigProperties fallback,
      OtelSpringProperties otelSdkProperties) {
    this.environment = environment;
    this.prefix = prefix;
    this.fallback = fallback;
    this.otelSdkProperties = otelSdkProperties;
  }

  @Nullable
  @Override
  public String getString(String name) {
    return or(environment.getProperty(fullKey(name), String.class), fallback.getString(name));
  }

  @Nullable
  @Override
  public Boolean getBoolean(String name) {
    return or(environment.getProperty(fullKey(name), Boolean.class), fallback.getBoolean(name));
  }

  @Nullable
  @Override
  public Integer getInt(String name) {
    return or(environment.getProperty(fullKey(name), Integer.class), fallback.getInt(name));
  }

  @Nullable
  @Override
  public Long getLong(String name) {
    return or(environment.getProperty(fullKey(name), Long.class), fallback.getLong(name));
  }

  @Nullable
  @Override
  public Double getDouble(String name) {
    return or(environment.getProperty(fullKey(name), Double.class), fallback.getDouble(name));
  }

  private String fullKey(String name) {
    return prefix + "." + name;
  }

  @Override
  public Set<String> getPropertyKeys() {
    return environment.getPropertySources().stream()
        .flatMap(
            source ->
                source instanceof EnumerablePropertySource<?>
                    ? Arrays.stream(((EnumerablePropertySource<?>) source).getPropertyNames())
                    : Stream.empty())
        .filter(name -> name.startsWith(prefix + "."))
        .collect(Collectors.toSet());
  }

  @Nullable
  @Override
  public List<StructuredConfigProperties> getStructuredList(String name) {
    if ("otlp.headers".equals(fullKey(name))) {
      // this means, we mirror the entire model tree
      // therefore, it seems to be better te tweak git@github.com:joelittlejohn/jsonschema2pojo.git
      // to add the required @ConfigurationProperties(prefix = "otel") annotations
      // (add an org.jsonschema2pojo.Annotator) - and maybe a bit more
      List<OtelSpringProperties.OtlpHeader> otlpHeaders = otelSdkProperties.getOtlpHeaders();
    }

    return null;
//    return or(environment.getProperty(name, List.class), otelSdkProperties.getList(name));
  }

  @Nullable
  private static <T> T or(@Nullable T first, @Nullable T second) {
    return first != null ? first : second;
  }
}
