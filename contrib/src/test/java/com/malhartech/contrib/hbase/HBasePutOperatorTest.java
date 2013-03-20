/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.malhartech.contrib.hbase;

import com.malhartech.api.DAG;
import com.malhartech.stram.StramLocalCluster;
import org.apache.hadoop.hbase.client.Put;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pramod Immaneni <pramod@malhar-inc.com>
 */
public class HBasePutOperatorTest
{
  private static final Logger logger = LoggerFactory.getLogger(HBasePutOperatorTest.class);

  public HBasePutOperatorTest()
  {
  }

  @Test
  public void testPut()
  {
    try {
      HBaseTestHelper.clearHBase();
      DAG dag = new DAG();
      HBaseRowTupleGenerator rtg = dag.addOperator("tuplegenerator", HBaseRowTupleGenerator.class);
      TestHBasePutOperator thop = dag.addOperator("testhbaseput", TestHBasePutOperator.class);
      dag.addStream("ss", rtg.outputPort, thop.inputPort);

      thop.setTableName("table1");
      thop.setZookeeperQuorum("127.0.0.1");
      thop.setZookeeperClientPort(2822);

      StramLocalCluster lc = new StramLocalCluster(dag);
      lc.setHeartbeatMonitoringEnabled(false);
      lc.run(10000);
      /*
      tuples = new ArrayList<HBaseTuple>();
      TestHBaseScanOperator thop = new TestHBaseScanOperator();
           thop.setTableName("table1");
      thop.setZookeeperQuorum("127.0.0.1");
      thop.setZookeeperClientPort(2822);
      thop.setupConfiguration();

      thop.emitTuples();
      */

      // TODO review the generated test code and remove the default call to fail.
      //fail("The test case is a prototype.");
      // Check total number
      HBaseTuple tuple = HBaseTestHelper.getHBaseTuple("row0", "colfam0", "col-0");
      assert tuple != null;
      assert tuple.getRow().equals("row0");
      assert tuple.getColFamily().equals("colfam0");
      assert tuple.getColName().equals("col-0");
      assert tuple.getColValue().equals("val-0-0");
      tuple = HBaseTestHelper.getHBaseTuple("row499", "colfam0", "col-0");
      assert tuple != null;
      assert tuple.getRow().equals("row499");
      assert tuple.getColFamily().equals("colfam0");
      assert tuple.getColName().equals("col-0");
      assert tuple.getColValue().equals("val-499-0");
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      assert false;
    }
  }

  public static class TestHBasePutOperator extends HBasePutOperator<HBaseTuple> {

    @Override
    public Put operationPut(HBaseTuple t)
    {
      Put put = new Put(t.getRow().getBytes());
      put.add(t.getColFamily().getBytes(), t.getColName().getBytes(), t.getColValue().getBytes());
      return put;
    }

  }
}
