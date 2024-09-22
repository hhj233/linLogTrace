package org.slf4j;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.spi.MDCAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author linzj
 */
public class LogTraceTtlAdapter implements MDCAdapter {
    private final TransmittableThreadLocal<Map<String,String>> copyOnThreadLocal = new TransmittableThreadLocal();

    private static final int WRITE_OPERATION = 1;
    private static final int MAP_COPY_OPERATION = 2;

    final ThreadLocal<Integer> lastOperation = new ThreadLocal();

    static LogTraceTtlAdapter logTraceTtlAdapter;

    public LogTraceTtlAdapter() {
    }

    static {
        logTraceTtlAdapter = new LogTraceTtlAdapter();
        MDC.mdcAdapter = logTraceTtlAdapter;
    }

    public static MDCAdapter getInstance() {
        return logTraceTtlAdapter;
    }

    private Integer getAndSetLastOperation(int op) {
        Integer lastOp = (Integer)this.lastOperation.get();
        this.lastOperation.set(op);
        return lastOp;
    }

    private boolean wasLastOpReadOrNull(Integer lastOp) {
        return lastOp == null || lastOp == MAP_COPY_OPERATION;
    }

    private Map<String, String> duplicateAndInsertNewMap(Map<String, String> oldMap) {
        Map<String, String> newMap = Collections.synchronizedMap(new HashMap());
        if (oldMap != null) {
            synchronized(oldMap) {
                newMap.putAll(oldMap);
            }
        }

        this.copyOnThreadLocal.set(newMap);
        return newMap;
    }

    @Override
    public void put(String key, String val) throws IllegalArgumentException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        } else {
            Map<String, String> oldMap = (Map)this.copyOnThreadLocal.get();
            Integer lastOp = this.getAndSetLastOperation(WRITE_OPERATION);
            if (!this.wasLastOpReadOrNull(lastOp) && oldMap != null) {
                oldMap.put(key, val);
            } else {
                Map<String, String> newMap = this.duplicateAndInsertNewMap(oldMap);
                newMap.put(key, val);
            }

        }
    }

    @Override
    public void remove(String key) {
        if (key != null) {
            Map<String, String> oldMap = (Map)this.copyOnThreadLocal.get();
            if (oldMap != null) {
                Integer lastOp = this.getAndSetLastOperation(WRITE_OPERATION);
                if (this.wasLastOpReadOrNull(lastOp)) {
                    Map<String, String> newMap = this.duplicateAndInsertNewMap(oldMap);
                    newMap.remove(key);
                } else {
                    oldMap.remove(key);
                }

            }
        }
    }

    @Override
    public void clear() {
        this.lastOperation.set(WRITE_OPERATION);
        this.copyOnThreadLocal.remove();
    }

    @Override
    public String get(String key) {
        Map<String, String> map = (Map)this.copyOnThreadLocal.get();
        return map != null && key != null ? (String)map.get(key) : null;
    }

    public Map<String, String> getPropertyMap() {
        this.lastOperation.set(2);
        return (Map)this.copyOnThreadLocal.get();
    }

    public Set<String> getKeys() {
        Map<String, String> map = this.getPropertyMap();
        return map != null ? map.keySet() : null;
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        Map<String, String> hashMap = (Map)this.copyOnThreadLocal.get();
        return hashMap == null ? null : new HashMap(hashMap);
    }

    @Override
    public void setContextMap(Map<String, String> contextMap) {
        this.lastOperation.set(WRITE_OPERATION);
        Map<String, String> newMap = Collections.synchronizedMap(new HashMap());
        newMap.putAll(contextMap);
        this.copyOnThreadLocal.set(newMap);
    }
}
