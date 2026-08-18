package egovframework.workcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * WorkCare Guide 백엔드 애플리케이션의 실행 진입점이다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EgovBootApplication {

    /**
     * Java 애플리케이션을 실행하고 Spring 컨테이너와
     * 내장 Tomcat을 시작한다.
     *
     * @param args 애플리케이션 실행 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(EgovBootApplication.class, args);
    }
}