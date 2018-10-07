/*
 * Copyright (c) 2010-2020, wandalong (hnxyhcwdl1003@163.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.perf4j.log4j2;


import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.helpers.AcceptableRangeConfiguration;
import org.perf4j.helpers.MiscUtils;
import org.perf4j.helpers.StatisticsExposingMBean;

/**
 * This appender is designed to be attached to an {@link AsyncCoalescingStatisticsAppender}. It takes the incoming
 * GroupedTimingStatistics log messages and uses this data to update the value of a JMX MBean. The attributes on this
 * MBean can then be monitored by external tools. In addition, this class allows you to specify notification thresholds
 * so that a JMX notification is sent if one of the attributes falls outside an acceptable range (for example, if
 * the mean time for a specific value is too high).
 */
@Plugin(name = "JmxAttributes", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class JmxAttributeStatisticsAppender extends AbstractAppender {
   
	@PluginFactory
    public static JmxAttributeStatisticsAppender createAppender(
            @PluginElement("Layout") final Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions) {
        return new JmxAttributeStatisticsAppender(name, filter, layout, ignoreExceptions);
    }
	
	protected JmxAttributeStatisticsAppender(String name, Filter filter,
			Layout<? extends Serializable> layout) {
		super(name, filter, layout);
	}

	protected JmxAttributeStatisticsAppender(String name, Filter filter,
			Layout<? extends Serializable> layout, boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions);
	}
	
 
	// --- configuration options ---
    /**
     * The object name of the MBean exposed through the JMX server.
     */
    private String mBeanName = StatisticsExposingMBean.DEFAULT_MBEAN_NAME;
    /**
     * A comma separated list of the tag names to be exposed as JMX attributes.
     */
    private String tagNamesToExpose;
    /**
     * A comma separated list of the notification thresholds, which controls whether JMX notifications are sent
     * when attribute values fall outside acceptable ranges.
     */
    private String notificationThresholds;

    // --- state variables ---
    /**
     * This is the MBean that is registered with the MBeanServer
     */
    protected StatisticsExposingMBean mBean;

    // --- options ---
    /**
     * The <b>MBeanName</b> option is used to specify the ObjectName under which the StatisticsExposingMBean in the
     * MBeanServer. If not specified, defaults to org.perf4j:type=StatisticsExposingMBean,name=Perf4J.
     *
     * @return The value of the MBeanName option
     */
    public String getMBeanName() {
        return mBeanName;
    }

    /**
     * Sets the value of the <b>MBeanName</b> option. This must be a valid JMX ObjectName.
     *
     * @param mBeanName The new value for the MBeanName option.
     */
    public void setMBeanName(String mBeanName) {
        this.mBeanName = mBeanName;
    }

    /**
     * The <b>TagNamesToExpose</b> option is a comma-separated list of the tag names whose statistics values (e.g.
     * mean, min, max, etc.) should be exposed as MBeanAttributes. See the
     * {@link org.perf4j.helpers.StatisticsExposingMBean} for more details.
     *
     * @return The value of the TagNamesToExpose expose
     */
    public String getTagNamesToExpose() {
        return tagNamesToExpose;
    }

    /**
     * Sets the value of the TagNamesToExpose option.
     *
     * @param tagNamesToExpose The new value for the TagNamesToExpose option.
     */
    public void setTagNamesToExpose(String tagNamesToExpose) {
        this.tagNamesToExpose = tagNamesToExpose;
    }

    /**
     * The <b>NotificationThresholds</b> option is a comma-separated list of <i>acceptable range configurations</i>.
     * An acceptable range configuration specifies the values for which a particular timing statistic is considered
     * good. If the statistic falls outside of this range, then a JMX notification will be sent.
     * <p>
     * The format of an acceptable range configuration is <tt>tagNameStatName(range)</tt> where range can be one of
     * <tt>&lt;value</tt>, <tt>&gt;value</tt>, or <tt>minValue-maxValue</tt>. For example, suppose the
     * TagNamesToExpose option was set to "databaseCall,fileWrite". This would cause the generated MBean to
     * expose the following attributes:
     * <ul>
     *   <li>databaseCallMean
     *   <li>databaseCallStdDev
     *   <li>databaseCallMin
     *   <li>databaseCallMax
     *   <li>databaseCallCount
     *   <li>databaseCallTPS
     *   <li>fileWriteMean
     *   <li>fileWriteStdDev
     *   <li>fileWriteMin
     *   <li>fileWriteMax
     *   <li>fileWriteCount
     *   <li>fileWriteTPS
     * </ul>
     * Suppose you wanted to have a JMX notification sent if the databaseCallMean is ever greater than 100ms, the
     * databaseCallMax is ever greater than 1000ms, the fileWriteMean is ever less than 5ms or greater than 200ms,
     * and the fileWriteTPS is ever less than 1 transaction per second. You would specify a NotificationThreshold as:
     * <pre>databaseCallMean(<100),databaseCallMax(<1000),fileWriteMean(5-200),fileWriteTPS(>1)</pre>
     *
     * @return The value of the NotificationThresholds option
     */
    public String getNotificationThresholds() {
        return notificationThresholds;
    }

    /**
     * Sets the value of the NotificationThresholds option.
     *
     * @param notificationThresholds The new value for the NotificationThresholds option.
     */
    public void setNotificationThresholds(String notificationThresholds) {
        this.notificationThresholds = notificationThresholds;
    }

    public void activateOptions() {
        if (tagNamesToExpose == null) {
            throw new RuntimeException("You must set the TagNamesToExpose option before activating this appender");
        }

        //parse the options, create the mBean and register it
        String[] tagNames = MiscUtils.splitAndTrim(tagNamesToExpose, ",");

        List<AcceptableRangeConfiguration> rangeConfigs = new ArrayList<AcceptableRangeConfiguration>();
        if (notificationThresholds != null) {
            String[] rangeConfigStrings = MiscUtils.splitAndTrim(notificationThresholds, ",");
            for (String rangeConfigString : rangeConfigStrings) {
                rangeConfigs.add(new AcceptableRangeConfiguration(rangeConfigString));
            }
        }
        
        mBean = new StatisticsExposingMBean(mBeanName, Arrays.asList(tagNames), rangeConfigs);

        try {
            MBeanServer mBeanServer = getMBeanServer();
            mBeanServer.registerMBean(mBean, new ObjectName(mBeanName));
        } catch (Exception e) {
            throw new RuntimeException("Error registering statistics MBean: " + e.getMessage(), e);
        }
    }

    // --- appender interface methods ---

    public void append(LogEvent event) {
        Object logMessage = event.getMessage();
        if (logMessage instanceof GroupedTimingStatistics && mBean != null) {
            mBean.updateCurrentTimingStatistics((GroupedTimingStatistics) logMessage);
        }
    }

    public boolean requiresLayout() {
        return false;
    }

    public void close() {
        try {
            MBeanServer mBeanServer = getMBeanServer();
            mBeanServer.unregisterMBean(new ObjectName(mBeanName));
        } catch (Exception e) {
            //fine, if we can't unregister it's not a big deal
        }
    }

    // --- helper methods ---
    /**
     * Gets the MBeanServer that should be used to register the StatisticsExposingMBean. Defaults to the Java Platform
     * MBeanServer. Subclasses could override this to use a different server.
     *
     * @return The MBeanServer to use for registrations.
     */
    protected MBeanServer getMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

   
}
