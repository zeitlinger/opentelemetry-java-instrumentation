/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.opamp;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

public class OpAmpClient {

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(OpAmpClient.class.getName());

  public static final String CONFIG_DIR =
      "/home/gregor/source/spring-boot-opentelemetry-demo/opamp";

  private final WatchService watchService;
  private final DynamicLogLevels dynamicLogLevels;

  private boolean poll = true;

  public OpAmpClient(WatchService watchService, DynamicLogLevels dynamicLogLevels) {
    this.watchService = watchService;
    this.dynamicLogLevels = dynamicLogLevels;
  }

  public static OpAmpClient create() {
    try {
      WatchService watchService = FileSystems.getDefault().newWatchService();
      Path path = Paths.get(CONFIG_DIR);
      path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
      OpAmpClient client = new OpAmpClient(watchService, new DynamicLogLevels());
      client.init();
      client.load();
      new Thread(client::poll).start();
      return client;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void init() {
    dynamicLogLevels.init();
  }

  public void poll() {
    while (poll) {
      try {
        WatchKey key = watchService.take();
        for (WatchEvent<?> ignored : key.pollEvents()) {
          load();
        }
        poll = key.reset();
      } catch (InterruptedException e) {
        logger.log(java.util.logging.Level.INFO, "Interrupted while waiting for file change", e);
        return;
      }
    }
  }

  private void load() {
    try {
      Yaml yaml = new Yaml(new Constructor(OpAmpConfig.class, new LoaderOptions()));
      Path path = Paths.get(CONFIG_DIR, "config.yaml");
      OpAmpConfig config = yaml.load(new FileInputStream(path.toFile()));

      dynamicLogLevels.applyLogLevels(config.logLevels);
    } catch (FileNotFoundException e) {
      logger.log(java.util.logging.Level.INFO, "File not found", e);
    } catch (YAMLException e) {
      logger.log(java.util.logging.Level.INFO, "Error parsing config", e);
    }
  }
}
