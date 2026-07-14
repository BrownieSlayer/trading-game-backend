package app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import app.configuration.TestcontainersConfiguration;

@SpringBootTest(classes = TradingGameBackendApplication.class)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TradingGameBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
