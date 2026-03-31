package org.springframework.samples.petclinic.conf;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * This class provides configuration for springdoc-openapi to generate a testing client
 * in the user interface (Swagger UI).
 *  
 * This bean is used if 'petclinic.security.enable' property has been set to TRUE. 
 * => The generated swagger user interface will have a button to define username and password in the call.
 */
@Configuration
@ConditionalOnProperty(name = "petclinic.security.enable", havingValue = "true")
public class ApiDocSecurityEnabledConfig {
    
    @Bean
    public GroupedOpenApi petclinicApi() {
        return GroupedOpenApi.builder()
                .group("petclinic")
                .packagesToScan("org.springframework.samples.petclinic")
                .pathsToMatch("/petclinic/api/**")
                .build();
    }

    @Bean
    public OpenAPI petclinicOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Pet Clinic Application Reactive (Secured)")
                        .description("Leveraging Reactive Datastax driver and Astra Cassandra-as-a-service")
                        .version("1.0.0-SNAPSHOT")
                        .termsOfService("Terms of service")
                        .contact(new Contact()
                                .name("DataStax Examples")
                                .url("https://www.datastax.com/examples")
                                .email("examples@datastax.com"))
                        .license(new License()
                                .name("Apache v2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")));
    }

}
