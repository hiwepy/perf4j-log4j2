package org.perf4j.log4j2.aop;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.aspectj.lang.annotation.Aspect;
import org.perf4j.aop.AbstractTimingAspect;
import org.perf4j.log4j2.Log4J2StopWatch;

/**
 * This TimingAspect implementation uses Log4j2 to persist StopWatch log messages.
 */
@Aspect
public class TimingAspect extends AbstractTimingAspect {
    
	protected Log4J2StopWatch newStopWatch(String loggerName, String levelName) {
        Level level = Level.toLevel(levelName, Level.INFO);
        return new Log4J2StopWatch(LogManager.getLogger(loggerName), level, level);
    }
	
}

