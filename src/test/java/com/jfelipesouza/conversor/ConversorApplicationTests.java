package com.jfelipesouza.conversor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azure.storage.blob.BlobContainerClient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
@AutoConfigureMockMvc
class ConversorApplicationTests {
	@MockitoBean
	BlobContainerClient blobContainerClient;

	@Autowired
	MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void rejectsUnknownRequestFieldsAndInvalidFormats() throws Exception {
		mockMvc.perform(post("/conversor")
				.contentType("application/json")
				.content("""
						{"url":"https://www.youtube.com/watch?v=test","type":"MP3","command":"whoami"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request is invalid."));

		mockMvc.perform(post("/conversor")
				.contentType("application/json")
				.content("""
						{"url":"https://www.youtube.com/watch?v=test","type":"WAV"}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsNotFoundForUnknownDownloadTokens() throws Exception {
		mockMvc.perform(get("/downloads/not-a-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Download link was not found or has expired."));
	}
}
