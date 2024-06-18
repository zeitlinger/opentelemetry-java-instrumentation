/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v3_8.client;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientPeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.netty.common.internal.NettyConnectionRequest;
import io.opentelemetry.instrumentation.netty.common.internal.NettyErrorHolder;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;
import io.opentelemetry.javaagent.bootstrap.internal.HttpClientInstrumenterFactory;
import io.opentelemetry.javaagent.instrumentation.netty.v3_8.HttpRequestAndChannel;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpResponse;

public final class NettyClientSingletons {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.netty-3.8";

  private static final Instrumenter<HttpRequestAndChannel, HttpResponse> INSTRUMENTER;
  private static final Instrumenter<NettyConnectionRequest, Channel> CONNECTION_INSTRUMENTER;

  static {
    INSTRUMENTER =
        HttpClientInstrumenterFactory.builder(new NettyHttpClientAttributesGetter())
            .instrumenterBuilder(INSTRUMENTATION_NAME)
            .addContextCustomizer(
                (context, requestAndChannel, startAttributes) -> NettyErrorHolder.init(context))
            .buildClientInstrumenter(HttpRequestHeadersSetter.INSTANCE);

    CONNECTION_INSTRUMENTER =
        Instrumenter.<NettyConnectionRequest, Channel>builder(
                GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME, NettyConnectionRequest::spanName)
            .addAttributesExtractor(
                HttpClientAttributesExtractor.create(NettyConnectHttpAttributesGetter.INSTANCE))
            .addAttributesExtractor(
                HttpClientPeerServiceAttributesExtractor.create(
                    NettyConnectHttpAttributesGetter.INSTANCE,
                    CommonConfig.get().getPeerServiceResolver()))
            .buildInstrumenter(SpanKindExtractor.alwaysClient());
  }

  public static Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter() {
    return INSTRUMENTER;
  }

  public static Instrumenter<NettyConnectionRequest, Channel> connectionInstrumenter() {
    return CONNECTION_INSTRUMENTER;
  }

  private NettyClientSingletons() {}
}
