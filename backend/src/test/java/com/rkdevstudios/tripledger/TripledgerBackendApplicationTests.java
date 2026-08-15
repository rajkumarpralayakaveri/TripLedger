package com.rkdevstudios.tripledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"cloudinary.cloud-name=test-cloud",
		"cloudinary.api-key=test-key",
		"cloudinary.api-secret=test-secret"
})
class TripledgerBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
