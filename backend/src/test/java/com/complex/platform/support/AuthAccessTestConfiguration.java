// 文件版本：V0.01
package com.complex.platform.support;

import com.complex.platform.access.AccessRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** MVC 切片不加载持久层，显式提供员工停权查询依赖。 */
@TestConfiguration
public class AuthAccessTestConfiguration {
    @Bean
    AccessRepository authAccessRepository() {
        return Mockito.mock(AccessRepository.class);
    }
}
