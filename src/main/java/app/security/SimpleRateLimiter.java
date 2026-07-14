package app.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class SimpleRateLimiter {
    
    private final Map<String, List<Long>> requestTimestamps = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 5;
    private static final long TIME_WINDOW_MS = 60_000; // 1 minute
    
    public boolean allowRequest(String clientId) {
        long now = System.currentTimeMillis();
        
        requestTimestamps.putIfAbsent(clientId, new ArrayList<>());
        List<Long> timestamps = requestTimestamps.get(clientId);
        
        // Nettoyer les anciennes entrées
        timestamps.removeIf(timestamp -> now - timestamp > TIME_WINDOW_MS);
        
        if (timestamps.size() >= MAX_REQUESTS) {
            return false;
        }
        
        timestamps.add(now);
        return true;
    }
}