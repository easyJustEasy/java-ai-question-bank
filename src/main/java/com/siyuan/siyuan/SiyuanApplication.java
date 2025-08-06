package com.siyuan.siyuan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude =  {DataSourceAutoConfiguration.class})
public class SiyuanApplication {

	public static void main(String[] args) {
		SpringApplication.run(SiyuanApplication.class, args);
	}

}
