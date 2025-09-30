package no.softwarecontrol.idoc.webservices.restapi.ratelimit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Provider
@RateLimit

public class RateLimitFilter implements ContainerRequestFilter {
    @Context
    private ResourceInfo resourceInfo;

    private final LoadingCache<String, AtomicInteger> requestCountsCache;

    private final java.util.concurrent.ConcurrentMap<Integer, LoadingCache<String, AtomicInteger>> cachesByWindow =
            new java.util.concurrent.ConcurrentHashMap<>();

    public RateLimitFilter() {
        requestCountsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public AtomicInteger load(String key) {
                        return new AtomicInteger(0);
                    }
                });
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        RateLimit rateLimit = resourceInfo.getResourceMethod().getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = resourceInfo.getResourceClass().getAnnotation(RateLimit.class);
        }

        String clientIp = requestContext.getHeaderString("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = requestContext.getUriInfo().getRequestUri().getHost();
        }

        // Lag nøkkel per klient + endepunkt + HTTP-metode, så ikke forskjellige endepunkter deler kvote
        String endpointId = resourceInfo.getResourceClass().getName() + "#" +
                resourceInfo.getResourceMethod().getName();
        String httpMethod = requestContext.getMethod();
        String key = clientIp + "|" + httpMethod + "|" + endpointId;

        try {
            LoadingCache<String, AtomicInteger> cache = getCache(rateLimit.seconds());
            AtomicInteger counter = cache.get(key);
            if (counter.incrementAndGet() > rateLimit.requests()) {
                requestContext.abortWith(Response
                        .status(Response.Status.TOO_MANY_REQUESTS)
                        .entity("Rate limit exceeded. Try again later.")
                        .build());
            }
        } catch (Exception e) {
            // Valgfritt: logging
        }

    }

    private LoadingCache<String, AtomicInteger> getCache(int seconds) {
        return cachesByWindow.computeIfAbsent(seconds, s ->
                CacheBuilder.newBuilder()
                        .expireAfterWrite(s, TimeUnit.SECONDS)
                        .build(new CacheLoader<>() {
                            @Override
                            public AtomicInteger load(String key) {
                                return new AtomicInteger(0);
                            }
                        })
        );
    }


}
