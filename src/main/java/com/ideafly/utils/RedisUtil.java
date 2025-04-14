package com.ideafly.utils;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Component
@Slf4j
@SuppressWarnings("unchecked")
public class RedisUtil {

    @Qualifier("ideaFlyRedisTemplate")
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 向redis写入key-value【string类型】
     *
     * @param key
     * @param value
     */
    public <T> void set(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 向redis写入key-value【string类型】
     *
     * @param key
     * @param value
     * @param expireTime 毫秒，过期时间
     */
    public <T> void set(String key, T value, long expireTime) {
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.MILLISECONDS);
    }

    public <T> void mSet(Map<String, T> kvs, long expireMillis) {
        if (kvs == null || kvs.isEmpty()) {
            return;
        }
//        redisTemplate.opsForValue().multiSet(kvs);
//        for (Map.Entry<String, T> e : kvs.entrySet()) {
//            set(e.getKey(), e.getValue(), expireMillis);
//        }
        // 开启事务支持
        redisTemplate.setEnableTransactionSupport(true);
        redisTemplate.multi();
        // 遍历存入，值随便定义
        for (Map.Entry<String, T> e : kvs.entrySet()) {
            set(e.getKey(), e.getValue(), expireMillis);
        }
        redisTemplate.exec();
    }

    public <T> void sAdd(String key, long expireTime, T... values) {
        expire(key, expireTime);
        redisTemplate.opsForSet().add(key, values);
    }

    public <T> boolean sIsMember(String key, T value) {
        Boolean result = redisTemplate.opsForSet().isMember(key, value);
        return result != null && result;
    }

    public <T> void sAdd(String key, Collection<T> values, long expireTime) {
        if (values == null || values.isEmpty()) {
            return;
        }
        sAdd(key, expireTime, values.toArray());
    }

    public <T> void mSAdd(Map<String, Collection<T>> kvs, long expireTime) {
        if (kvs == null || kvs.isEmpty()) {
            return;
        }
        // 开启事务支持
        redisTemplate.setEnableTransactionSupport(true);
        redisTemplate.multi();
        // 遍历存入，值随便定义
        for (Map.Entry<String, Collection<T>> e : kvs.entrySet()) {
            sAdd(e.getKey(), e.getValue(), expireTime);
        }
        redisTemplate.exec();
    }

    public <T> Set<T> sMembers(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null) {
            return Collections.emptySet();
        }
        return (Set<T>) members;
    }

    public <T> void srm(String key, T... values) {
        redisTemplate.opsForSet().remove(key, values);
    }

    public <T> void srm(String key, Collection<T> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        srm(key, values.toArray());
    }

    /**
     * 从redis读取key的值【string类型】
     *
     * @param key
     * @return Object
     */
    public <T> T get(String key) {
        return key == null ? null : (T) redisTemplate.opsForValue().get(key);
    }

    public <T> List<T> mGet(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> results = redisTemplate.opsForValue().multiGet(keys);
        return results == null ? Collections.emptyList() : (List<T>) results;
    }

    /**
     * Hash类型读取方法
     *
     * @param key     键 不能为null
     * @param hashKey 项 不能为null
     * @return T
     */
    public <T, HK> T hGet(String key, HK hashKey) {
        return (T) redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public <HK, HV> Map<HK, HV> hGetAll(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        return (Map<HK, HV>) entries;
    }

    public <HK, HV> List<HV> hMGet(String key, Collection<HK> hashKeys) {
        if (hashKeys == null || hashKeys.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<Object> keys = (Collection<Object>)(Collection<?>) hashKeys;
        List<Object> results = redisTemplate.opsForHash().multiGet(key, keys);
        return results == null ? Collections.emptyList() : (List<HV>) results;
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public <HV> List<HV> hGetAllValues(String key) {
        List<Object> values = redisTemplate.opsForHash().values(key);
        return values == null ? Collections.emptyList() : (List<HV>) values;
    }

    public <HK> Set<HK> hGetAllKeys(String key) {
        Set<Object> keys = redisTemplate.opsForHash().keys(key);
        return keys == null ? Collections.emptySet() : (Set<HK>) keys;
    }

    /**
     * 向hash中放入数据,如果不存在将创建
     *
     * @param key     键
     * @param hashKey 项
     * @param value   值
     */
    public <T, HK> void hSet(String key, HK hashKey, T value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 批量设置hash
     *
     * @param key   键
     * @param value 对应多个键值
     * @param time  时间(毫秒)
     * @return true成功 false失败
     */
    public <HK, HV> boolean hMSet(String key, Map<HK, HV> value, long time) {
        Map<Object, Object> map = (Map<Object, Object>)(Map<?, ?>) value;
        redisTemplate.opsForHash().putAll(key, map);
        if (time > 0) {
            expire(key, time);
        }
        return true;
    }

    /**
     * 向hash中放入数据,如果不存在将创建
     *
     * @param key        键
     * @param hashKey    项
     * @param value      值
     * @param expireTime 有效时间，按key设，每次插入元素都更新有效期
     */
    public <T, HK> void hSet(String key, HK hashKey, T value, long expireTime) {
        expire(key, expireTime);
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * Hash类型读取方法
     *
     * @param key     键 不能为null
     * @param hashKey 项 不能为null
     * @return T
     */
    public Boolean hHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    /**
     * 向队列中放入数据，最大为max个
     *
     * @param key
     * @param value
     * @param max
     */
    public <T> void lSet(String key, T value, Long max) {
        //删除已经存在的
        redisTemplate.opsForList().remove(key, 0, value);
        Long count = redisTemplate.opsForList().leftPush(key, value);
        if (max != null && count != null && count > max) {
            redisTemplate.opsForList().rightPop(key);
        }
    }

    public <T> List<T> lGet(String key, long max) {
        List<Object> list = redisTemplate.opsForList().range(key, 0, max);
        return list == null ? Collections.emptyList() : (List<T>) list;
    }
    
    public Long lRemove(String key, long max, Object value) {
        return redisTemplate.opsForList().remove(key, max, value);
    }
    
    public <T> void lLeftPush(String key, T t) {
        redisTemplate.opsForList().leftPush(key, t);
    }
    
    public <T> void lLeftPushAll(String key, T... values) {
        redisTemplate.opsForList().leftPushAll(key, values);
    }
    
    public <T> void lRightPush(String key, T t) {
        redisTemplate.opsForList().rightPush(key, t);
    }
    
    public <T> void lRightPushAll(String key, List<T> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<Object> objects = (List<Object>)(List<?>) values;
        redisTemplate.opsForList().rightPushAll(key, objects);
    }
    
    public <T> T lRightPop(String key) {
        return (T) redisTemplate.opsForList().rightPop(key);
    }
    
    public <T> T lLeftPop(String key) {
        return (T) redisTemplate.opsForList().leftPop(key);
    }
    
    /**
     * 批量添加数据
     *
     * @param keyList    键
     * @param expireTime 有效时间
     */
    public void batchSet(List<String> keyList, long expireTime) {
        // 开启事务支持
        redisTemplate.setEnableTransactionSupport(true);
        redisTemplate.multi();
        // 遍历存入，值随便定义
        keyList.forEach(key -> {
            set(key, 1, expireTime);
        });
        redisTemplate.exec();
    }

    /**
     * 批量删除数据
     *
     * @param keys
     */
    public void mDel(List<String> keys) {
        redisTemplate.multi();
        // 遍历存入，值随便定义
        keys.forEach(this::del);
        redisTemplate.exec();
    }

    /**
     * 批量删除hash中的值
     *
     * @param key      键 不能为null
     * @param hashKeys 项 可以是多个， 不能为null
     */
    public void hdel(String key, Object... hashKeys) {
        redisTemplate.opsForHash().delete(key, hashKeys);
    }

    /**
     * 删除
     *
     * @param key 键 ，不能为null
     */
    public void del(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 设置过期时间
     *
     * @param key
     * @param expireTime 毫秒
     */
    public void expire(String key, long expireTime) {
        redisTemplate.expire(key, expireTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 递增
     *
     * @param key
     * @return long
     */
    public Long incr(String key, long expireTime) {
        // 自增，并返回自增后的结果
        Long value = redisTemplate.opsForValue().increment(key);
        // 第一次使用时设置过期时间
        if (value != null && value == 1L) {
            expire(key, expireTime);
        }
        return value;
    }

    /**
     * 递减
     *
     * @param key
     * @return long
     */
    public Long decr(String key) {
        // 递减
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 分布式锁，加锁成功返回true，否则false
     *
     * @param key
     * @param value
     * @param expireTime 毫秒，过期时间
     */
    public Boolean lock(String key, String value, long expireTime) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 解锁
     *
     * @param key
     * @param value
     * @return boolean
     */
    public Boolean releaseLock(String key, String value) {
        // lua脚本：比较加锁值，相等则删除
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        // 实例化DefaultRedisScript对象
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        // 设置返回结果类型
        script.setResultType(Boolean.class);
        // 执行lua脚本
        return redisTemplate.execute(script, Arrays.asList(key), value);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获取批量keys对应的列表中，指定的hash键值对列表
     * @param keys redis 键
     * @param hashKeys 哈希表键的集合（你需要获取的那些键）
     * @return
     */
    public <T, H> List<Map<H, T>> mHGet(List<String> keys, List<H> hashKeys) {
        List<Map<H, T>> hashList = new ArrayList<>();
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            List<byte[]> hkbs = hashKeys.stream()
                .map(k -> k.toString().getBytes())
                .collect(Collectors.toList());
            for (String key : keys) {
                connection.hMGet(key.getBytes(), hkbs.toArray(new byte[0][0]));
            }
            return null;
        });
        
        if (results == null) {
            return hashList;
        }
        
        for (Object hv : results) {
            if (Objects.isNull(hv) || !(hv instanceof List)) {
                continue;
            }
            List<Object> list = (List<Object>) hv;
            if (CollectionUtil.isEmpty(list)) {
                continue;
            }
            List<T> items = list.stream()
                .map(o -> (T) o)
                .collect(Collectors.toList());
            Map<H, T> map = new LinkedHashMap<>();
            for (int i = 0; i < items.size(); i++) {
                map.put(hashKeys.get(i), items.get(i));
            }
            hashList.add(map);
        }
        return hashList;
    }
}
