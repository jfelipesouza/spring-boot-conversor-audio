package com.jfelipesouza.conversor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azure.storage.blob.BlobContainerClient;
import tools.jackson.databind.ObjectMapper;

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

	@Autowired
	ObjectMapper objectMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void rejectsUnknownRequestFieldsAndInvalidFormats() throws Exception {
		mockMvc.perform(post("/conversor")
				.contentType("application/json")
				.content("""
						{"url":"https://www.youtube.com/watch?v=test","type":"MP3","filename":"safe.mp3","command":"whoami"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("A requisição é inválida."))
				.andExpect(jsonPath("$.error").value("Requisição inválida"));

		mockMvc.perform(post("/conversor")
				.contentType("application/json")
				.content("""
						{"url":"https://www.youtube.com/watch?v=test","type":"WAV","filename":"safe.mp3"}
						"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/conversor")
				.contentType("application/json")
				.content("""
						{"url":"https://www.youtube.com/watch?v=test","type":"MP3"}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsNotFoundForUnknownDownloadTokens() throws Exception {
		mockMvc.perform(get("/downloads/not-a-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("O link de download não foi encontrado ou expirou."));
	}

	@Test
	void acceptsFilenameInLowercaseAndTheExistingCamelCaseAlias() throws Exception {
		var lowercase = objectMapper.readValue("""
				{"url":"https://www.youtube.com/watch?v=test","type":"MP3","filename":"lowercase.mp3"}
				""", com.jfelipesouza.conversor.dto.ConversorRequestBody.class);
		var camelCase = objectMapper.readValue("""
				{"url":"https://www.youtube.com/watch?v=test","type":"MP3","fileName":"camel-case.mp3"}
				""", com.jfelipesouza.conversor.dto.ConversorRequestBody.class);

		org.junit.jupiter.api.Assertions.assertEquals("lowercase.mp3", lowercase.filename());
		org.junit.jupiter.api.Assertions.assertEquals("camel-case.mp3", camelCase.filename());
	}

	@Test
	void documentsThePortugueseFilenameAndResponseSchemas() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/conversor'].post.summary").value("Converter áudio do YouTube para MP3"))
				.andExpect(jsonPath("$.components.schemas.ConversorRequestBody.properties.filename.description")
						.value("Nome desejado para o arquivo MP3. A extensão .mp3 é opcional."))
				.andExpect(jsonPath("$.components.schemas.ConversorResponse.properties.status.example")
						.value("CONCLUÍDO"));
	}
}
