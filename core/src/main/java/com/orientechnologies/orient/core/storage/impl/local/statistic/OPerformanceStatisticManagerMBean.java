package com.orientechnologies.orient.core.storage.impl.local.statistic;

import javax.management.*;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JMX bean which allows to start/stop monitoring of OrientDB performance
 * characteristics and exposes all system and component wide performance attributes of {@link OPerformanceStatisticManager}.
 * <p>
 * System wide attributes has the same MBean attribute names, but for each component wide attribute MBean attribute name consist of
 * "attribute name" {@link #COMPONENT_SEPARATOR} "component name".
 */
public class OPerformanceStatisticManagerMBean implements DynamicMBean {
  /**
   * Name of "pagesPerOperation" performance attribute
   */
  public static final String PAGES_PER_OPERATION = "pagesPerOperation";

  /**
   * Name of "cacheHits" performance attribute
   */
  public static final String CACHE_HITS = "cacheHits";

  /**
   * Separator in property name between component name and name of performance attribute
   */
  public static final String COMPONENT_SEPARATOR = "_";

  /**
   * Name of "commitTime" performance attribute
   */
  public static final String COMMIT_TIME = "commitTime";

  /**
   * Name of "readSpeedFromCache" performance attribute
   */
  public static final String READ_SPEED_FROM_CACHE = "readSpeedFromCache";

  /**
   * Name of "readSpeedFromFile" performance attribute
   */
  public static final String READ_SPEED_FROM_FILE = "readSpeedFromFile";

  /**
   * Name of "writeSpeedInCache" performance attribute
   */
  public static final String WRITE_SPEED_IN_CACHE = "writeSpeedInCache";

  /**
   * Name of "startMonitoring" method
   */
  public static final String START_MONITORING = "startMonitoring";

  /**
   * Name of "stopMonitoring" method
   */
  public static final String STOP_MONITORING = "stopMonitoring";

  /**
   * Reference to related performance manager
   */
  private final OPerformanceStatisticManager manager;

  public OPerformanceStatisticManagerMBean(OPerformanceStatisticManager manager) {
    this.manager = manager;
  }

  @Override
  public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
    if (attribute == null) {
      throw new RuntimeOperationsException(new IllegalArgumentException("Attribute name cannot be null"),
          "Cannot invoke a getter of " + getClass().getSimpleName() +
              " with null attribute name");
    }

    final String[] parameters = attribute.split("\\Q" + COMPONENT_SEPARATOR + "\\E");
    if (parameters.length > 2) {
      throw new RuntimeOperationsException(new IllegalArgumentException("Invalid attribute name"),
          "Class " + getClass().getSimpleName() +
              " does not contain attributes with name " + attribute);
    }

    //if attribute name does not include component name it is system wide not component wide attribute
    if (parameters[0].equals(CACHE_HITS)) {
      if (parameters.length == 1)
        return manager.getCacheHits();
      else {
        return manager.getCacheHits(parameters[1]);
      }
    } else if (parameters[0].equals(COMMIT_TIME)) {
      return manager.getCommitTimeAvg();
    } else if (parameters[0].equals(READ_SPEED_FROM_CACHE)) {
      if (parameters.length == 1)
        return manager.getReadSpeedFromCacheInPages();
      else
        return manager.getReadSpeedFromCacheInPages(parameters[1]);
    } else if (parameters[0].equals(READ_SPEED_FROM_FILE)) {
      if (parameters.length == 1)
        return manager.getReadSpeedFromFileInPages();
      else
        return manager.getReadSpeedFromFileInPages(parameters[1]);
    } else if (parameters[0].equals(WRITE_SPEED_IN_CACHE)) {
      if (parameters.length == 1)
        return manager.getWriteSpeedInCacheInPages();
      else
        return manager.getWriteSpeedInCacheInPages(parameters[1]);
    } else if (parameters[0].equals(PAGES_PER_OPERATION)) {
      if (parameters.length == 1)
        throw new RuntimeOperationsException(new IllegalArgumentException("Unknown attribute"),
            "Amount of pages per operation is measured only on component level");

      return manager.getAmountOfPagesPerOperation(parameters[1]);
    }

    throw (new AttributeNotFoundException("Cannot find " + attribute + " attribute in " + getClass().getSimpleName()));
  }

  @Override
  public void setAttribute(Attribute attribute)
      throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
    throwAreNotSupported();
  }

  private void throwAreNotSupported() {
    throw new RuntimeOperationsException(new UnsupportedOperationException("Modification operations are not supported"),
        "You can not apply modification operations to performance attributes");
  }

  @Override
  public AttributeList getAttributes(String[] attributes) {
    if (attributes == null) {
      throw new RuntimeOperationsException(new IllegalArgumentException("attributeNames[] cannot be null"),
          "Cannot invoke a getter of " + getClass().getSimpleName());
    }
    AttributeList resultList = new AttributeList();

    if (attributes.length == 0)
      return resultList;

    // build the result attribute list
    for (String attribute : attributes) {
      try {
        Object value = getAttribute((String) attribute);
        resultList.add(new Attribute(attribute, value));
      } catch (Exception e) {
        // print debug info but continue processing list
        e.printStackTrace();
      }
    }

    return resultList;
  }

  @Override
  public AttributeList setAttributes(AttributeList attributes) {
    throwAreNotSupported();
    return new AttributeList();
  }

  @Override
  public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
    if (actionName == null) {
      throw new RuntimeOperationsException(new IllegalArgumentException("Operation name cannot be null"),
          "Cannot invoke a null operation in " + getClass().getSimpleName());
    }

    if (actionName.equals(START_MONITORING)) {
      manager.startMonitoring();
      return null;
    }

    if (actionName.equals(STOP_MONITORING)) {
      manager.stopMonitoring();
      return null;
    }

    throw new ReflectionException(new NoSuchMethodException(actionName), "Cannot find the operation " + actionName +
        " in " + getClass().getSimpleName());
  }

  @Override
  public MBeanInfo getMBeanInfo() {
    final List<MBeanAttributeInfo> performanceAttributes = new ArrayList<MBeanAttributeInfo>();
    populatePerformanceAttributes(performanceAttributes);

    final List<MBeanOperationInfo> operations = new ArrayList<MBeanOperationInfo>();

    final MBeanOperationInfo startMonitoring = new MBeanOperationInfo(START_MONITORING,
        "Starts monitoring OrientDB performance characteristics", new MBeanParameterInfo[0], void.class.getName(),
        MBeanOperationInfo.ACTION);
    operations.add(startMonitoring);

    final MBeanOperationInfo stopMonitoring = new MBeanOperationInfo(STOP_MONITORING,
        "Stops monitoring OrientDB performance characteristics", new MBeanParameterInfo[0], void.class.getName(),
        MBeanOperationInfo.ACTION);
    operations.add(stopMonitoring);

    return new MBeanInfo(this.getClass().getName(), "MBean to monitor OrientDB performance characteristics",
        performanceAttributes.toArray(new MBeanAttributeInfo[performanceAttributes.size()]), new MBeanConstructorInfo[0],
        operations.toArray(new MBeanOperationInfo[operations.size()]), new MBeanNotificationInfo[0]);
  }

  private void populatePerformanceAttributes(final List<MBeanAttributeInfo> performanceAttributes) {
    final Collection<String> components = manager.getComponentNames();

    populateCacheHits(performanceAttributes, components);
    populateCommitTime(performanceAttributes);
    populateReadSpeedFromCache(performanceAttributes, components);
    populateReadSpeedFromFile(performanceAttributes, components);
    populateWriteSpeedInCache(performanceAttributes, components);
    populatePagesPerOperation(performanceAttributes, components);
  }

  private void populateWriteSpeedInCache(List<MBeanAttributeInfo> performanceAttributes, Collection<String> components) {
    final MBeanAttributeInfo writeSpeedInCache = new ModelMBeanAttributeInfo(WRITE_SPEED_IN_CACHE, long.class.getName(),
        "Write speed to disk cache in pages per second", true, false, false);
    performanceAttributes.add(writeSpeedInCache);

    for (String component : components) {
      final MBeanAttributeInfo componentWriteSpeedInCache = new ModelMBeanAttributeInfo(
          WRITE_SPEED_IN_CACHE + COMPONENT_SEPARATOR + component, long.class.getName(),
          "Write speed to disk cache in pages per second for component " + component, true, false, false);
      performanceAttributes.add(componentWriteSpeedInCache);
    }
  }

  private void populateReadSpeedFromFile(List<MBeanAttributeInfo> performanceAttributes, Collection<String> components) {
    final MBeanAttributeInfo readSpeedFromFile = new ModelMBeanAttributeInfo(READ_SPEED_FROM_FILE, long.class.getName(),
        "Read speed from file system in pages per second", true, false, false);
    performanceAttributes.add(readSpeedFromFile);

    for (String component : components) {
      final MBeanAttributeInfo componentReadSpeedFromFile = new ModelMBeanAttributeInfo(
          READ_SPEED_FROM_FILE + COMPONENT_SEPARATOR + component, long.class.getName(),
          "Read speed from file system in pages per second for component " + component, true, false, false);
      performanceAttributes.add(componentReadSpeedFromFile);
    }
  }

  private void populateReadSpeedFromCache(List<MBeanAttributeInfo> performanceAttributes, Collection<String> components) {
    final MBeanAttributeInfo readSpeedFromCache = new ModelMBeanAttributeInfo(READ_SPEED_FROM_CACHE, long.class.getName(),
        "Read speed from disk cache in pages per second", true, false, false);
    performanceAttributes.add(readSpeedFromCache);

    for (String component : components) {
      final MBeanAttributeInfo componentReadSpeedFromCache = new ModelMBeanAttributeInfo(
          READ_SPEED_FROM_CACHE + COMPONENT_SEPARATOR + component, long.class.getName(),
          "Read speed from disk cache in pages per second for component " + component, true, false, false);
      performanceAttributes.add(componentReadSpeedFromCache);
    }
  }

  private void populateCommitTime(List<MBeanAttributeInfo> performanceAttributes) {
    final MBeanAttributeInfo commitTime = new ModelMBeanAttributeInfo(COMMIT_TIME, long.class.getName(),
        "Average commit time in nanoseconds", true, false, false);
    performanceAttributes.add(commitTime);
  }

  private void populateCacheHits(List<MBeanAttributeInfo> performanceAttributes, Collection<String> components) {
    final MBeanAttributeInfo cacheHits = new MBeanAttributeInfo(CACHE_HITS, int.class.getName(),
        "Cache hits of read disk cache in percents", true, false, false);
    performanceAttributes.add(cacheHits);

    for (String component : components) {
      final MBeanAttributeInfo componentCacheHits = new ModelMBeanAttributeInfo(CACHE_HITS + COMPONENT_SEPARATOR + component,
          int.class.getName(), "Cache hits of read disc cache for component " + component + " in percents", true, false, false);
      performanceAttributes.add(componentCacheHits);
    }
  }

  private void populatePagesPerOperation(List<MBeanAttributeInfo> performanceAttributes, Collection<String> components) {
    for (String component : components) {
      final MBeanAttributeInfo componentCacheHits = new ModelMBeanAttributeInfo(
          PAGES_PER_OPERATION + COMPONENT_SEPARATOR + component, int.class.getName(),
          "Average amount of pages per operation for component " + component, true, false, false);
      performanceAttributes.add(componentCacheHits);
    }
  }
}
