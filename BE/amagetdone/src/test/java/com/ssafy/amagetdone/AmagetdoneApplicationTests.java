package com.ssafy.amagetdone;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"DB_URL=jdbc:postgresql://localhost:5432/amagetdone",
	"DB_USERNAME=postgres",
	"DB_PASSWORD=postgres"
})
class AmagetdoneApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoadsWithDataSource() {
		assertNotNull(dataSource);
	}

}
