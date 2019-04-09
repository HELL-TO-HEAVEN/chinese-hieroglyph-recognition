package MLP;

import MLP.configuration.StorageProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * author: ElinaValieva on 15.12.2018
 */
@SpringBootApplication
@EnableConfigurationProperties(StorageProperty.class)
public class HieroglyphRecognitionApplication {

    public static void main(String[] args) {
        SpringApplication.run(HieroglyphRecognitionApplication.class, args);
    }
}
