package org.sterl.training.redis;

import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.hash.ObjectHashMapper;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.Nullable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.sterl.training.redis.entity.CachedEntity;
import org.sterl.training.redis.service.entity.Person;

import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestObjectHashMapper {

    @Autowired
    ReactiveRedisConnectionFactory factory;
    
    @Autowired
    ObjectHashMapper objectMapper;

    @Test
    void test() {
        Person p = new Person("1", "Foo");
        final Map<byte[], byte[]> hash = objectMapper.toHash(p);
        System.out.println(hash);
    }

    enum ByteArrayRedisSerializer implements RedisSerializer<byte[]> {
        INSTANCE;
        @Nullable
        @Override
        public byte[] serialize(@Nullable byte[] bytes) throws SerializationException {
            return bytes;
        }
        @Nullable
        @Override
        public byte[] deserialize(@Nullable byte[] bytes) throws SerializationException {
            return bytes;
        }
    }

    @Test
    void testReactiveObjectMapper() throws Exception {
        final StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisSerializationContext.RedisSerializationContextBuilder<String, byte[]> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

              RedisSerializationContext<String, byte[]> context = builder
                      .value(ByteArrayRedisSerializer.INSTANCE)
                      .hashKey(ByteArrayRedisSerializer.INSTANCE)
                      .hashValue(ByteArrayRedisSerializer.INSTANCE)
                      .build();
        

        final ReactiveRedisTemplate<String, byte[]> template = new ReactiveRedisTemplate<>(factory, context);
        final ReactiveHashOperations<String, byte[], byte[]> opsForHash = template.opsForHash();
        
        final CachedEntity entry = CachedEntity.builder()
                .id("Muster_id")
                .payload("Muster")
                .cacheTime(Instant.now())
                .build();
        
        // save
        Mono.just(entry)
            .flatMap(e -> opsForHash.putAll("entity:" + e.getId(), objectMapper.toHash(e)))
            .block();
        
        opsForHash.entries("entity:Muster_id")
            .collectMap(Entry::getKey, Entry::getValue)
            .subscribe(m -> {
                final Object result = objectMapper.fromHash(m);
                System.out.println(result);
            });
    }

}