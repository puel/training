package org.sterl.training.redis.model;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@RedisHash("entity")
@AllArgsConstructor @NoArgsConstructor @Data @Builder
public class CachedEntity implements Serializable {

    @Id
    private String id;
    private String payload;
    private Instant cacheTime;
}
