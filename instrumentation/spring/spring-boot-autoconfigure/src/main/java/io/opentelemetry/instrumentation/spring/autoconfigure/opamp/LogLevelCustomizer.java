/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.opamp;

import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.LogRecordData;

@SuppressWarnings("SystemOut")
public class LogLevelCustomizer {

  private LogLevelCustomizer() {}

  public static LogRecordProcessor customizeLogRecordProcessor(
      LogRecordProcessor delegate, OpAmpClient client) {
    return new LogRecordProcessor() {
      @Override
      public void onEmit(Context context, ReadWriteLogRecord record) {
        if (shouldEmit(record.toLogRecordData(), client)) {
          delegate.onEmit(context, record);
        }
      }

      @Override
      public CompletableResultCode shutdown() {
        return delegate.shutdown();
      }

      @Override
      public CompletableResultCode forceFlush() {
        return delegate.forceFlush();
      }

      @Override
      public void close() {
        delegate.close();
      }
    };
  }

  private static boolean shouldEmit(LogRecordData data, OpAmpClient client) {
    String logger = data.getInstrumentationScopeInfo().getName();
    Severity severity = data.getSeverity();

    DynamicLogLevels.LogDecision decision = client.getLogLevels().getLogDecision(logger);
    Severity wantSeverity = decision.getSeverity();

    if (wantSeverity != null && severity.getSeverityNumber() < wantSeverity.getSeverityNumber()) {
      return false;
    }

    return !dropNonSampled(decision, severity);
  }

  private static boolean dropNonSampled(DynamicLogLevels.LogDecision decision, Severity severity) {
    Severity samplingSeverity = decision.getSamplingSeverity();
    return samplingSeverity != null
        && severity.getSeverityNumber() <= samplingSeverity.getSeverityNumber()
        && Span.current().getSpanContext().getTraceFlags() != TraceFlags.getSampled();
  }
}
