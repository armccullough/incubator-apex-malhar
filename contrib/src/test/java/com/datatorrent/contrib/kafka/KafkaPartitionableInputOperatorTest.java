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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import junit.framework.Assert;
import com.datatorrent.api.BaseOperator;
import com.datatorrent.api.DAG;
import com.datatorrent.api.DefaultInputPort;
import com.datatorrent.api.LocalMode;
import com.datatorrent.api.Operator;
import com.datatorrent.api.Context.OperatorContext;
import com.datatorrent.api.DAG.Locality;

/**
 * A test to verify the input operator will be automated partitoned according to the kafka partition
 */
public class KafkaPartitionableInputOperatorTest extends KafkaOperatorTestBase
{
  
  public KafkaPartitionableInputOperatorTest()
  {
    // This class want to initialize several kafka brokers for multiple partitions
    hasMultiPartition = true;
  }
  
  static final org.slf4j.Logger logger = LoggerFactory.getLogger(KafkaPartitionableInputOperatorTest.class);
  static HashMap<String, List<?>> collections = new HashMap<String, List<?>>();
  static AtomicInteger tupleCount = new AtomicInteger();
  static CountDownLatch latch;

  /**
   * Test Operator to collect tuples from KafkaSingleInputStringOperator.
   * 
   * @param <T>
   */
  public static class CollectorModule<T> extends BaseOperator
  {
    public final transient CollectorInputPort<T> inputPort = new CollectorInputPort<T>("myInput", this);
  }

  public static class CollectorInputPort<T> extends DefaultInputPort<T>
  {
    ArrayList<T> list;
    final String id;

    public CollectorInputPort(String id, Operator module)
    {
      super();
      this.id = id;
    }

    @Override
    public void process(T tuple)
    {
      if (tuple.equals(KafkaOperatorTestBase.END_TUPLE)) {
        if (latch != null) {
          latch.countDown();
        }
        return;
      }
      list.add(tuple);
      tupleCount.incrementAndGet();
    }

    @Override
    public void setConnected(boolean flag)
    {
      if (flag) {
        collections.put(id, list = new ArrayList<T>());
      }
    }
  }
  
  /**
   * Test AbstractKafkaSinglePortInputOperator (i.e. an input adapter for
   * Kafka, aka consumer). This module receives data from an outside test
   * generator through Kafka message bus and feed that data into Malhar
   * streaming platform.
   * 
   * [Generate message and send that to Kafka message bus] ==> [Receive that
   * message through Kafka input adapter(i.e. consumer) and send using
   * emitTuples() interface on output port during onMessage call]
   * 
   * 
   * @throws Exception
   */
  @Test
  public void testKafkaInputOperator() throws Exception
  {
    // initial the latch for this test, there are 2 partitions with 2 collectors so wait till both partitions receive end_tuple
    latch = new CountDownLatch(2);
    
    int totalCount = 10000;
    
 // Start producer
    KafkaTestProducer p = new KafkaTestProducer("topic1", true);
    p.setSendCount(totalCount);
    new Thread(p).start();

    // Create DAG for testing.
    LocalMode lma = LocalMode.newInstance();
    DAG dag = lma.getDAG();

    // Create KafkaSinglePortStringInputOperator
    PartitionableKafkaSinglePortStringInputOperator node = dag.addOperator("Kafka message consumer", PartitionableKafkaSinglePortStringInputOperator.class);
    node.setConsumer(new SimpleKafkaConsumer());
    node.getConsumer().setTopic("topic1");
    
    // Set the 
    dag.setAttribute(node, OperatorContext.INITIAL_PARTITION_COUNT, 1);

    // Create Test tuple collector
    CollectorModule<String> collector = dag.addOperator("TestMessageCollector", new CollectorModule<String>());

    // Connect ports
    dag.addStream("Kafka message", node.outputPort, collector.inputPort).setLocality(Locality.CONTAINER_LOCAL);

    // Create local cluster
    final LocalMode.Controller lc = lma.getController();
    lc.setHeartbeatMonitoringEnabled(false);

    lc.runAsync();
    
    // Wait 30s for consumer finish consuming all the messages
    Assert.assertTrue("TIMEOUT: 30s ", latch.await(30000, TimeUnit.MILLISECONDS));
    
    // Check results
    Assert.assertEquals("Collections size", 1, collections.size());
    Assert.assertEquals("Tuple count", totalCount, collections.get(collector.inputPort.id).size());
    logger.debug(String.format("Number of emitted tuples: %d", collections.get(collector.inputPort.id).size()));
    
    p.close();
    lc.shutdown();
  }
  
}
