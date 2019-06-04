package com.sequenceiq.cloudbreak.logger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("CloudbreakLogContextAspects")
@Aspect
public class LogContextAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogContextAspects.class);

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.service.events.CloudbreakEventHandler.accept(..))")
    public void interceptCloudbreakEventHandlerAcceptMethod() {
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptCloudbreakEventHandlerAcceptMethod()")
    public void buildLogContextForReactorHandler(JoinPoint joinPoint) {
        com.sequenceiq.flow.logger.LogContextAspects.buildLogContextForEventHandler(joinPoint);
        LOGGER.debug("A Reactor event handler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }
}
