/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.opamp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

public class OpAmpClient {

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(OpAmpClient.class.getName());
  public static final String AGENT_URL = "http://localhost:12345";

  private final DynamicLogLevels logLevels;
  private final DynamicSampler sampler;
  private final String url;

  private boolean poll = true;

  public OpAmpClient(String url, DynamicLogLevels logLevels, DynamicSampler sampler) {
    this.url = url;
    this.logLevels = logLevels;
    this.sampler = sampler;
  }

  public static OpAmpClient create(DynamicSampler sampler, String serviceName) {
    OpAmpClient client =
        new OpAmpClient(
            String.format("%s/api/v0/debugdial/%s", AGENT_URL, serviceName),
            new DynamicLogLevels(),
            sampler);
    client.init();
    client.load();
    new Thread(client::poll).start();
    return client;
  }

  private void init() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> poll = false));
    logLevels.init();
  }

  public void poll() {
    while (poll) {
      try {
        Thread.sleep(10000);
        load();
      } catch (InterruptedException e) {
        logger.log(java.util.logging.Level.INFO, "Interrupted while waiting for file change", e);
        return;
      }
    }
  }

  private void load() {
    try {
      try (InputStream is = new URL(AGENT_URL + "/-/reload").openStream()) {
        byte[] data = new byte[16384];

        while (is.read(data, 0, data.length) != -1) {}
      }
      try (InputStream inputStream = new URL(url).openStream()) {
        Yaml yaml = new Yaml(new Constructor(OpAmpConfig.class, new LoaderOptions()));
        OpAmpConfig config = yaml.load(inputStream);

        logLevels.applyLogLevels(config.logLevels);
        sampler.setRatio(config.sampleRatio);
      }
    } catch (YAMLException e) {
      logger.log(java.util.logging.Level.INFO, "Error parsing config", e);
    } catch (IOException e) {
      logger.log(java.util.logging.Level.INFO, "Error reading config", e);
    }
  }
}
