/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.opamp;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import javax.annotation.Nullable;

public final class DynamicSampler implements Sampler {
  private static final SamplingResult POSITIVE_SAMPLING_RESULT = SamplingResult.recordAndSample();
  private static final SamplingResult NEGATIVE_SAMPLING_RESULT = SamplingResult.drop();
  private long idUpperBound;
  private String description;

  public static DynamicSampler create(double ratio) {
    DynamicSampler sampler = new DynamicSampler();
    sampler.setRatio(ratio);
    return sampler;
  }

  private static long getIdUpperBound(double ratio) {
    long idUpperBound;
    if (ratio == 0.0) {
      idUpperBound = Long.MIN_VALUE;
    } else if (ratio == 1.0) {
      idUpperBound = Long.MAX_VALUE;
    } else {
      idUpperBound = (long) (ratio * 9.223372036854776E18);
    }
    return idUpperBound;
  }

  void setRatio(double ratio) {
    if (!(ratio < 0.0) && !(ratio > 1.0)) {
      this.idUpperBound = getIdUpperBound(ratio);
      this.description = "TraceIdRatioBased{" + decimalFormat(ratio) + "}";
    } else {
      throw new IllegalArgumentException("ratio must be in range [0.0, 1.0]");
    }
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    return Math.abs(getTraceIdRandomPart(traceId)) < this.idUpperBound
        ? POSITIVE_SAMPLING_RESULT
        : NEGATIVE_SAMPLING_RESULT;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof DynamicSampler)) {
      return false;
    } else {
      DynamicSampler that = (DynamicSampler) obj;
      return this.idUpperBound == that.idUpperBound;
    }
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.idUpperBound);
  }

  @Override
  public String toString() {
    return this.getDescription();
  }

  private static long getTraceIdRandomPart(String traceId) {
    return OtelEncodingUtils.longFromBase16String(traceId, 16);
  }

  private static String decimalFormat(double value) {
    DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
    decimalFormatSymbols.setDecimalSeparator('.');
    DecimalFormat decimalFormat = new DecimalFormat("0.000000", decimalFormatSymbols);
    return decimalFormat.format(value);
  }
}
