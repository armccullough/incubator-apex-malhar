/*
 *  Copyright (c) 2012 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.lib.math;

import com.malhartech.annotation.ModuleAnnotation;
import com.malhartech.annotation.PortAnnotation;
import com.malhartech.dag.AbstractModule;
import com.malhartech.dag.FailedOperationException;
import com.malhartech.dag.ModuleConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Takes in one stream via input port "data". At end of window sends range of all values
 * for each key and emits them on port "range"<p> <br> Values are stored in a
 * hash<br> This node only functions in a windowed stram application<br> Compile
 * time error processing is done on configuration parameters<br> input port
 * "data" must be connected<br> output port "sum" must be connected<br>
 * "windowed" has to be true<br> Run time error processing are emitted on _error
 * port. The errors are:<br> Value is not a supported type<br>
 * <br>
 * <b>Benchmarks</b>: Blast as many tuples
 *
 * @author amol
 */


@ModuleAnnotation(
        ports = {
  @PortAnnotation(name = ArithmeticRange.IPORT_DATA, type = PortAnnotation.PortType.INPUT),
  @PortAnnotation(name = ArithmeticRange.OPORT_RANGE, type = PortAnnotation.PortType.OUTPUT)
})
public class ArithmeticRange extends AbstractModule
{
  public static final String IPORT_DATA = "data";
  public static final String OPORT_RANGE = "range";
  private static Logger LOG = LoggerFactory.getLogger(ArithmeticRange.class);
  HashMap<String, Number> high = new HashMap<String, Number>();
  HashMap<String, Number> low = new HashMap<String, Number>();

  enum supported_type {INT, SHORT, LONG, DOUBLE, FLOAT, UNDEFINED};
  supported_type type;


   /**
   * Expected tuple schema. If left undefined, the first tuple dictates the schema.
   *
   */
  public static final String KEY_SCHEMA = "schema";

  /**
   * Process each tuple
   *
   * @param payload
   */
  @Override
  public void process(Object payload)
  {
       for (Map.Entry<String, Object> e: ((HashMap<String, Object>)payload).entrySet()) {
         Number tval = (Number) e.getValue(); // later on accept string
         String key = e.getKey();
         Number val = high.get(key);
         if (val == null) {
           switch (type) {
             case INT:
                 val = new Integer(tval.intValue());
                 break;
             case DOUBLE:
                  val = new Double(tval.doubleValue());
                  break;
             case LONG:
                 val = new Long(tval.longValue());
                 break;
             case SHORT:
                 val = new Short(tval.shortValue());
                 break;
             case FLOAT:
                 val = new Float(tval.floatValue());
                 break;
             default:
               // The first tuple dictates the remaining types
               if (tval instanceof Integer) {
                 val = new Integer(tval.intValue());
                 type = supported_type.INT;
               }
               else if (tval instanceof Double) {
                 val = new Double(tval.doubleValue());
                 type = supported_type.DOUBLE;
               }
               else if (tval instanceof Long) {
                 val = new Long(tval.longValue());
                 type = supported_type.LONG;
               }
               else if (tval instanceof Short) {
                 val = new Short(tval.shortValue());
                 type = supported_type.SHORT;
               }
               else if (tval instanceof Float) {
                 val = new Float(tval.floatValue());
                 type = supported_type.FLOAT;
               }
               else { // should not execute, should be an error tuple
                 val = new Double(tval.doubleValue());
                 type = supported_type.UNDEFINED;
               }
               break;
           }
           high.put(key, val);
           low.put(key, val);
         }
         else {
           boolean error = true;
           switch(type) {
             case INT:
               error = !(tval instanceof Integer);
               if (!error) {
                 if (val.intValue() < tval.intValue()) { // no need to touch "low" as old val is automatic low
                   high.put(key, tval);
                 }
                 else if (val.intValue() > tval.intValue()) {
                   low.put(key, tval);
                 }
               }
               break;
             case DOUBLE:
               error = !(tval instanceof Double);
               if (!error) {
                 if (val.doubleValue() < tval.doubleValue()) { // no need to touch "low" as old val is automatic low
                   high.put(key, tval);
                 }
                 else if (val.doubleValue() > tval.doubleValue()) {
                   low.put(key, tval);
                 }
               }
               break;
             case LONG:
               error = !(tval instanceof Long);
               if (!error) {
                 if (val.longValue() < tval.longValue()) { // no need to touch "low" as old val is automatic low
                   high.put(key, tval);
                 }
                 else if (val.longValue() > tval.longValue()) {
                   low.put(key, tval);
                 }
               }
               break;
             case SHORT:
               error = !(tval instanceof Short);
               if (!error) {
                 if (val.shortValue() < tval.shortValue()) { // no need to touch "low" as old val is automatic low
                   high.put(key, tval);
                 }
                 else if (val.shortValue() > tval.shortValue()) {
                   low.put(key, tval);
                 }
               }
               break;
             case FLOAT:
               error = !(tval instanceof Float);
               if (!error) {
                 if (val.floatValue() < tval.floatValue()) { // no need to touch "low" as old val is automatic low
                   high.put(key, tval);
                 }
                 else if (val.floatValue() > tval.floatValue()) {
                   low.put(key, tval);
                 }
               }
               break;
             default:
               break;
           }
           // if error emit this tuple on error port
         }
      }
  }

  public boolean myValidation(ModuleConfiguration config)
  {
    return true;
  }
   /**
   *
   * @param config
   */
  @Override
  public void setup(ModuleConfiguration config) throws FailedOperationException
  {
    if (!myValidation(config)) {
      throw new FailedOperationException("validation failed");
    }

    String str = config.get(KEY_SCHEMA, "");
    if (str.isEmpty()) {
      type = supported_type.UNDEFINED;
      str = "undefined";
    }
    else if (str == "integer") {
      type = supported_type.INT;
    }
    else if (str == "double") {
      type = supported_type.DOUBLE;
    }
    else if (str == "long") {
      type = supported_type.LONG;
    }
    else if (str == "short") {
      type = supported_type.SHORT;
    }
    else if (str == "float") {
      type = supported_type.FLOAT;
    }

    LOG.debug(String.format("Schema set to %s", str));
  }


  /**
   * Node only works in windowed mode. Emits all data upon end of window tuple
   */
  @Override
  public void endWindow()
  {
    HashMap<String, Object> tuples = new HashMap<String, Object>();
    for (Map.Entry<String, Number> e: high.entrySet()) {
      ArrayList alist = new ArrayList();
      alist.add(e.getValue());
      alist.add(low.get(e.getKey())); // cannot be null
      tuples.put(e.getKey(), alist);
    }
    // Should allow users to send each key as a separate tuple to load balance
    // This is an aggregate node, so load balancing would most likely not be needed
    if (!tuples.isEmpty()) {
      emit(OPORT_RANGE, tuples);
    }
    high.clear();
    low.clear();
  }

  /**
   *
   * Checks for user specific configuration values<p>
   *
   * @param config
   * @return boolean
   */
  @Override
  public boolean checkConfiguration(ModuleConfiguration config)
  {
    boolean ret = true;
    // TBD
    return ret && super.checkConfiguration(config);
  }
}
