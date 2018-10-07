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
package org.perf4j.log4j2.aop;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.perf4j.aop.AbstractEjbTimingAspect;
import org.perf4j.log4j2.Log4J2StopWatch;

/**
 * This TimingAspect implementation uses Log4j2 to persist StopWatch log messages.
 * To use this interceptor in your code, you should add this class name to the {@link javax.interceptor.Interceptors}
 * annotation on the EJB to be profiled.
 */
public class EjbTimingAspect extends AbstractEjbTimingAspect {
	
    protected Log4J2StopWatch newStopWatch(String loggerName, String levelName) {
        Level level = Level.toLevel(levelName, Level.INFO);
        return new Log4J2StopWatch(LogManager.getLogger(loggerName), level, level);
    }
    
}