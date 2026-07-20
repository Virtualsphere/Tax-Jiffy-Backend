package com.gst_reconsilation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

@SpringBootApplication
public class GstReconsilationApplication {

	public static void main(String[] args) {
		SpringApplication.run(GstReconsilationApplication.class, args);
	}

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, false);
        return module;
    }

}
