/*
 * Copyright (c) 2013 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datatorrent.contrib.kafka;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import kafka.utils.Utils;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;

/**
 * This is a base class setup/clean Kafka testing environment for all the input/output test 
 */
public class KafkaOperatorTestBase
{
  
  public static final String END_TUPLE = "END_TUPLE";
  public static final int TEST_ZOOKEEPER_PORT =  2182;
  public static final int TEST_KAFKA_BROKER1_PORT =  9092;
  public static final int TEST_KAFKA_BROKER2_PORT =  9093;
  
  static final org.slf4j.Logger logger = LoggerFactory.getLogger(KafkaOperatorTestBase.class);
  // since Kafka 0.8 use KafkaServerStatble instead of KafkaServer
  private KafkaServerStartable kserver;
  // it wont be initialized unless hasMultiPartition is set to true
  private KafkaServerStartable kserver2;
  private NIOServerCnxnFactory standaloneServerFactory;
  private final String zklogdir = "/tmp/zookeeper-server-data";
  private final String kafkalogdir = "/tmp/kafka-server-data";
  private final String kafkalogdir2 = "/tmp/kafka-server-data2";
  protected boolean hasMultiPartition = false; 
  
  
  public void startZookeeper()
  {
  
    try {
      int clientPort = TEST_ZOOKEEPER_PORT;
      int numConnections = 10;
      int tickTime = 2000;
      File dir = new File(zklogdir);
  
      ZooKeeperServer kserver = new ZooKeeperServer(dir, dir, tickTime);
      standaloneServerFactory = new NIOServerCnxnFactory();
      standaloneServerFactory.configure(new InetSocketAddress(clientPort), numConnections);
      standaloneServerFactory.startup(kserver); // start the zookeeper server.
      kserver.startup();
    }
    catch (InterruptedException ex) {
      logger.debug(ex.getLocalizedMessage());
    }
    catch (IOException ex) {
      logger.debug(ex.getLocalizedMessage());
    }
  }

  public void stopZookeeper()
  {
    standaloneServerFactory.shutdown();
    Utils.rm(zklogdir);
  }

  public void startKafkaServer()
  {
    Properties props = new Properties();
    props.setProperty("broker.id", "0");
    props.setProperty("log.dirs", kafkalogdir);
    props.setProperty("zookeeper.connect", "localhost:"+TEST_ZOOKEEPER_PORT);
    props.setProperty("port", ""+TEST_KAFKA_BROKER1_PORT);
    if(hasMultiPartition){
      props.setProperty("num.partitions", "2");
    } else {
      props.setProperty("num.partitions", "1");
    }
    props.setProperty("auto.create.topics.enable", "true");
    // set this to 50000 to boost the performance so most test data are in memory before flush to disk
    props.setProperty("log.flush.interval.messages", "50000");
    kserver = new KafkaServerStartable(new KafkaConfig(props));
    kserver.startup();
    if(hasMultiPartition){
      props.setProperty("broker.id", "1");
      props.setProperty("log.dirs", kafkalogdir2);
      props.setProperty("port", "" + TEST_KAFKA_BROKER2_PORT);
      props.setProperty("num.partitions", "2");
      kserver2 = new KafkaServerStartable(new KafkaConfig(props));
      kserver2.startup();
    }
    
  }

  public void stopKafkaServer()
  {
    if(hasMultiPartition){
      kserver2.shutdown();
      kserver2.awaitShutdown();
      Utils.rm(kafkalogdir2);
    }
    kserver.shutdown();
    kserver.awaitShutdown();
    Utils.rm(kafkalogdir);
  }

  @Before
  public void beforeTest()
  {
    try {
      startZookeeper();
      startKafkaServer();
    }
    catch (java.nio.channels.CancelledKeyException ex) {
      logger.debug("LSHIL {}", ex.getLocalizedMessage());
    }
  }

  @After
  public void afterTest()
  {
    try {
      stopKafkaServer();
      stopZookeeper();
    }
    catch (java.nio.channels.CancelledKeyException ex) {
      logger.debug("LSHIL {}", ex.getLocalizedMessage());
    }
  }
}

