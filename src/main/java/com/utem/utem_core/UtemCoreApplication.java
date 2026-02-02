package com.utem.utem_core;

import com.utem.utem_core.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class UtemCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(UtemCoreApplication.class, args);
	}

}
