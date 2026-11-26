package org.sy.baemin.common.id;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {

    // 생성기는 서버당 하나만 있어야 순번과 synchronized 락이 공유되어 ID가 겹치지 않는다.
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(@Value("${baemin.id.node-id}") long nodeId) {

        return new SnowflakeIdGenerator(nodeId);
    }

}
