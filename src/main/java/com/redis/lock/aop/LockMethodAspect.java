package com.redis.lock.aop;

import com.redis.lock.annotation.RedisLock;
import com.redis.lock.utils.JedisUtil;
import com.redis.lock.utils.RedisLockHelper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @description: AOP拦截器
 * @author: pjmike
 * @create: 2019/04/24 18:43
 */
@Aspect
@Component
public class LockMethodAspect {
    @Autowired
    private RedisLockHelper redisLockHelper;
    @Autowired
    private JedisUtil jedisUtil;
    private Logger logger = LoggerFactory.getLogger(LockMethodAspect.class);

    @Around("@annotation(com.redis.lock.annotation.RedisLock)")
    public Object around(ProceedingJoinPoint joinPoint) {
        Jedis jedis = jedisUtil.getJedis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RedisLock redisLock = method.getAnnotation(RedisLock.class);
        String value = UUID.randomUUID().toString();
        String key = redisLock.key();

        boolean isLock = false;
        Object result = null;
        try {
            isLock = redisLockHelper.lock(jedis,key, value, redisLock.expire(), redisLock.timeUnit());
            logger.info("isLock : {}",isLock);
            if (isLock) {
                result = joinPoint.proceed();
            }
        } catch (Throwable throwable) {
            logger.error("执行异常", throwable);
        } finally {
            if (isLock) {
                logger.info("释放锁");
                redisLockHelper.unlock(jedis,key, value);
            }
            jedis.close();
        }
        return result;
    }
}
