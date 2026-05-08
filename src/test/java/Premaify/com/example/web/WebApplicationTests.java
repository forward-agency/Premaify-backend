package Premaify.com.example.web;

import Premaify.com.example.web.model.Lead;
import Premaify.com.example.web.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class WebApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LeadRepository leadRepository;

	@BeforeEach
	void cleanLeads() {
		leadRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void summaryAliasReturnsDashboardSummary() throws Exception {
		mockMvc.perform(get("/summary").with(user("premaify@gmail.com").roles("SUPER_ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalProducts").exists())
				.andExpect(jsonPath("$.totalLeads").exists())
				.andExpect(jsonPath("$.activeListings").exists())
				.andExpect(jsonPath("$.inventoryCount").exists());
	}

	@Test
	void dashboardSummaryStillWorks() throws Exception {
		mockMvc.perform(get("/api/dashboard/summary").with(user("premaify@gmail.com").roles("SUPER_ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalProducts").exists());
	}

	@Test
	void adminSummaryRequiresLogin() throws Exception {
		mockMvc.perform(get("/summary"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void staffCanViewLeadsButCannotManageProductsOrInventory() throws Exception {
		mockMvc.perform(get("/api/leads").with(user("staff@premaify.com").roles("STAFF")))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/products")
						.with(user("staff@premaify.com").roles("STAFF"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/products/test-product/stock")
						.with(user("staff@premaify.com").roles("STAFF"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"stockQuantity\":5}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminAndStaffCanViewLeadsButCannotDeleteLeads() throws Exception {
		mockMvc.perform(get("/api/leads").with(user("admin@premaify.com").roles("ADMIN")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/leads").with(user("staff@premaify.com").roles("STAFF")))
				.andExpect(status().isOk());

		mockMvc.perform(delete("/api/leads").with(user("admin@premaify.com").roles("ADMIN")))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/leads").with(user("staff@premaify.com").roles("STAFF")))
				.andExpect(status().isForbidden());
	}

	@Test
	void leadsCanBeFilteredByStatusAndDateRange() throws Exception {
		Lead todayLead = new Lead();
		todayLead.setName("Today lead");
		todayLead.setPhone("9000000001");
		todayLead.setLaptop("Test laptop");
		todayLead.setStatus("Follow-Up");
		todayLead.setDate(LocalDate.now());
		leadRepository.save(todayLead);

		Lead olderLead = new Lead();
		olderLead.setName("Older lead");
		olderLead.setPhone("9000000002");
		olderLead.setLaptop("Old laptop");
		olderLead.setStatus("New");
		olderLead.setDate(LocalDate.now().minusDays(10));
		leadRepository.save(olderLead);

		mockMvc.perform(get("/api/leads")
						.with(user("admin@premaify.com").roles("ADMIN"))
						.param("status", "Follow-Up")
						.param("startDate", LocalDate.now().minusDays(1).toString())
						.param("endDate", LocalDate.now().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("Follow-Up"))
				.andExpect(jsonPath("$[0].name").value("Today lead"))
				.andExpect(jsonPath("$[1]").doesNotExist());
	}

	@Test
	void recentLeadsOnlyReturnsLeadsCreatedInLast48HoursNewestFirst() throws Exception {
		Lead newestLead = new Lead();
		newestLead.setName("Newest lead");
		newestLead.setPhone("9000000003");
		newestLead.setLaptop("New laptop");
		newestLead.setStatus("New");
		newestLead.setDate(LocalDate.now());
		newestLead.setCreatedAt(LocalDateTime.now().minusMinutes(5));
		leadRepository.save(newestLead);

		Lead yesterdayLead = new Lead();
		yesterdayLead.setName("Yesterday lead");
		yesterdayLead.setPhone("9000000004");
		yesterdayLead.setLaptop("Yesterday laptop");
		yesterdayLead.setStatus("New");
		yesterdayLead.setDate(LocalDate.now().minusDays(1));
		yesterdayLead.setCreatedAt(LocalDateTime.now().minusDays(1));
		leadRepository.save(yesterdayLead);

		Lead olderLead = new Lead();
		olderLead.setName("Older lead");
		olderLead.setPhone("9000000005");
		olderLead.setLaptop("Old laptop");
		olderLead.setStatus("New");
		olderLead.setDate(LocalDate.now().minusDays(5));
		olderLead.setCreatedAt(LocalDateTime.now().minusHours(49));
		leadRepository.save(olderLead);

		mockMvc.perform(get("/api/leads")
						.with(user("admin@premaify.com").roles("ADMIN"))
						.param("datePreset", "recent"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Newest lead"))
				.andExpect(jsonPath("$[1].name").value("Yesterday lead"))
				.andExpect(jsonPath("$[2]").doesNotExist());
	}
}
