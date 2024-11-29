package utils.cache;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisCache {
    private static final String RedisHostname = "cachescc242570567.redis.cache.windows.net";
    private static final String RedisKey = "kvq8m7NC3VvDLe4bX3dKF8zf0M4iJu3I5AzCaLkAT1o=";
    private static final int REDIS_PORT = 6380;
    private static final int REDIS_TIMEOUT = 1000;
    private static final boolean Redis_USE_TLS = true;

    private static JedisPool instance;

    public synchronized static JedisPool getCachePool() {
        if( instance != null)
            return instance;

        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        instance = new JedisPool(poolConfig, RedisHostname, REDIS_PORT, REDIS_TIMEOUT, RedisKey, Redis_USE_TLS);
        return instance;
    }
}