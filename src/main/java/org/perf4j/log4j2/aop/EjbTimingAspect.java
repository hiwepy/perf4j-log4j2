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