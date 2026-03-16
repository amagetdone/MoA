package com.ssafy.amagetdone.global.docs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class DocumentationEncodingTest {

	@Test
	void markdownDocsAreReadableAsUtf8() throws IOException {
		List<Path> markdownFiles;
		try (Stream<Path> stream = Files.walk(Path.of("docs"))) {
			markdownFiles = stream
				.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".md"))
				.toList();
		}

		assertFalse(markdownFiles.isEmpty());
		for (Path path : markdownFiles) {
			String content = assertDoesNotThrow(() -> Files.readString(path, StandardCharsets.UTF_8));
			assertFalse(content.contains("\uFFFD"), () -> "Broken replacement character found: " + path);
		}
	}

	@Test
	void swaggerDesignDocKeepsKoreanHeading() throws IOException {
		String content = Files.readString(
			Path.of("docs", "plans", "2026-03-16-swagger-korean-design.md"),
			StandardCharsets.UTF_8
		);

		assertTrue(content.contains("# Swagger \uD55C\uAE00\uD654 \uC124\uACC4"));
	}
}
