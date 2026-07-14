package app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import app.configuration.TestcontainersConfiguration;

@SpringBootTest(classes = IgnisBackendApplication.class)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class IgnisBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
