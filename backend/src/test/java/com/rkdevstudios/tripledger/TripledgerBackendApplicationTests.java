package com.rkdevstudios.tripledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.rkdevstudios.tripledger.expense.domain.ActivityEntry;
import com.rkdevstudios.tripledger.expense.domain.ActivityEntryRepository;
import com.rkdevstudios.tripledger.expense.persistence.JpaActivityEntryRepository;
import com.rkdevstudios.tripledger.expense.domain.ActivityType;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
		"cloudinary.cloud-name=test-cloud",
		"cloudinary.api-key=test-key",
		"cloudinary.api-secret=test-secret"
})
class TripledgerBackendApplicationTests {

	@Autowired
	private JpaActivityEntryRepository activityEntryRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void testPersistPaymentActivities() {
		String wsId = UUID.randomUUID().toString();
		String usrId = UUID.randomUUID().toString();

		// 1. Test PAYMENT_SUBMITTED
		String id1 = UUID.randomUUID().toString();
		ActivityEntry entry1 = new ActivityEntry(id1, wsId, usrId, ActivityType.PAYMENT_SUBMITTED, "{}");
		((ActivityEntryRepository) activityEntryRepository).save(entry1);
		
		ActivityEntry fetched1 = activityEntryRepository.findById(id1).orElse(null);
		assertNotNull(fetched1);
		assertEquals(ActivityType.PAYMENT_SUBMITTED, fetched1.getActivityType());

		// 2. Test PAYMENT_APPROVED
		String id2 = UUID.randomUUID().toString();
		ActivityEntry entry2 = new ActivityEntry(id2, wsId, usrId, ActivityType.PAYMENT_APPROVED, "{}");
		((ActivityEntryRepository) activityEntryRepository).save(entry2);

		ActivityEntry fetched2 = activityEntryRepository.findById(id2).orElse(null);
		assertNotNull(fetched2);
		assertEquals(ActivityType.PAYMENT_APPROVED, fetched2.getActivityType());

		// 3. Test PAYMENT_REJECTED
		String id3 = UUID.randomUUID().toString();
		ActivityEntry entry3 = new ActivityEntry(id3, wsId, usrId, ActivityType.PAYMENT_REJECTED, "{}");
		((ActivityEntryRepository) activityEntryRepository).save(entry3);

		ActivityEntry fetched3 = activityEntryRepository.findById(id3).orElse(null);
		assertNotNull(fetched3);
		assertEquals(ActivityType.PAYMENT_REJECTED, fetched3.getActivityType());
	}
}
