package utils.cache;

public class Cache {

    public static String get(String key) {
        /*var jedis = RedisCache.getCachePool().getResource();
        if(jedis.exists(key)){
            return jedis.get(key);
        } else{
            return null;
        }*/
        return null;
    }

    public static void put(String key, String value) {
        /*try{
            var jedis = RedisCache.getCachePool().getResource();

            jedis.set( key, value );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/
    }

    public static void replace(String key, String value){
        /*try {
            var jedis = RedisCache.getCachePool().getResource();
            if(jedis.exists(key)){
                jedis.del(key);
                jedis.set( key, value );
            } else{
                jedis.set( key, value );
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }*/
    }

    public static void delete(String key){
        /*try {
            var jedis = RedisCache.getCachePool().getResource();
            if(jedis.exists(key)) {
                jedis.del(key);
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }*/
    }

}
