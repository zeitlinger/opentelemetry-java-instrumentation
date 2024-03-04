/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.opamp;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import opamp.proto.Anyvalue;
import opamp.proto.Opamp;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class OpAmpClient {

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(OpAmpClient.class.getName());
  public static final String AGENT_URL = "http://localhost:12345";

  private final DynamicLogLevels logLevels;
  private final DynamicSampler sampler;
  private final String serviceInstanceId;
  private final String serviceName;
  private OpAmpConfig config;
  private final OkHttpClient okHttpClient;
  private final String url;
  private boolean poll = true;

  public OpAmpClient(
      String url,
      DynamicLogLevels logLevels,
      DynamicSampler sampler,
      String serviceInstanceId,
      String serviceName,
      OpAmpConfig config,
      OkHttpClient okHttpClient) {
    this.url = url;
    this.logLevels = logLevels;
    this.sampler = sampler;
    this.serviceInstanceId = serviceInstanceId;
    this.serviceName = serviceName;
    this.config = config;
    this.okHttpClient = okHttpClient;
  }

  public static OpAmpClient create(
      DynamicSampler sampler, String serviceName, String serviceInstanceId) {

    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    OpAmpClient client =
        new OpAmpClient(
            String.format("%s/api/v0/debugdial", AGENT_URL),
            DynamicLogLevels.create(),
            sampler,
            serviceInstanceId,
            serviceName,
            new OpAmpConfig(),
            okHttpClient);
    client.init();
    client.callOpAmpServer();
    new Thread(client::poll).start();
    return client;
  }

  public DynamicLogLevels getLogLevels() {
    return logLevels;
  }

  private void init() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> poll = false));
  }

  public void poll() {
    while (poll) {
      try {
        Thread.sleep(10000);
        callOpAmpServer();
      } catch (InterruptedException e) {
        logger.log(java.util.logging.Level.INFO, "Interrupted while waiting for file change", e);
        return;
      }
    }
  }

  private void callOpAmpServer() {
    try {
      try (InputStream is = new URL(AGENT_URL + "/-/reload").openStream()) {
        byte[] data = new byte[16384];

        while (is.read(data, 0, data.length) != -1) {}
      }
    } catch (IOException e) {
      logger.log(java.util.logging.Level.INFO, "Error reloading", e);
      return;
    }
    config.availableLoggers = logLevels.getAvailableLoggers();
    Opamp.AgentToServer agentToServer = getAgentToServer(serviceInstanceId, serviceName, config);

    Request.Builder requestBuilder = new Request.Builder().url(this.url);
    requestBuilder.post(
        okhttp3.RequestBody.create(
            agentToServer.toByteArray(), okhttp3.MediaType.parse("application/x-protobuf")));

    okHttpClient
        .newCall(requestBuilder.build())
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.log(Level.INFO, "Failed to send config to server", e);
              }

              @Override
              public void onResponse(@NotNull Call call, @NotNull Response response)
                  throws IOException {
                int code = response.code();
                if (code != 200) {
                  logger.log(Level.INFO, "Failed to send config to server, code: " + code);
                  return;
                }
                ResponseBody body = response.body();
                if (body == null) {
                  logger.info("Failed to send config to server, no response body");
                  return;
                }
                Opamp.ServerToAgent serverToAgent = Opamp.ServerToAgent.parseFrom(body.bytes());
                applyConfig(extractRemoteConfig(serverToAgent, OpAmpClient.this.serviceName));
              }
            });
  }

  private void applyConfig(OpAmpConfig config) {
    this.config = config;

    logLevels.applyLogLevels(config);
    sampler.setRatio(config.sampleRatio);
  }

  private static Yaml getYaml() {
    return new Yaml(new Constructor(OpAmpConfig.class, new LoaderOptions()));
  }

  private static Opamp.AgentToServer getAgentToServer(
      String serviceInstanceId, String serviceName, OpAmpConfig config) {
    String dump = getYaml().dump(config);

    return Opamp.AgentToServer.newBuilder()
        .setInstanceUid(serviceInstanceId)
        .setAgentDescription(
            Opamp.AgentDescription.newBuilder()
                .addIdentifyingAttributes(
                    Anyvalue.KeyValue.newBuilder()
                        .setKey("service.name")
                        .setValue(
                            Anyvalue.AnyValue.newBuilder().setStringValue(serviceName).build())
                        .build())
                .build())
        .setEffectiveConfig(
            Opamp.EffectiveConfig.newBuilder()
                .setConfigMap(
                    Opamp.AgentConfigMap.newBuilder()
                        .putConfigMap(
                            "java-config",
                            Opamp.AgentConfigFile.newBuilder()
                                .setContentType("application/yaml")
                                .setBody(ByteString.copyFrom(dump, StandardCharsets.UTF_8))
                                .build())
                        .build())
                .build())
        .build();
  }

  private static OpAmpConfig extractRemoteConfig(
      Opamp.ServerToAgent serverToAgent, String serviceName) {
    Opamp.AgentConfigFile agentConfigFile =
        serverToAgent.getRemoteConfig().getConfig().getConfigMapMap().get(serviceName);

    if (agentConfigFile == null) {
      return new OpAmpConfig();
    }

    return getYaml().load(agentConfigFile.getBody().toStringUtf8());
  }
}
