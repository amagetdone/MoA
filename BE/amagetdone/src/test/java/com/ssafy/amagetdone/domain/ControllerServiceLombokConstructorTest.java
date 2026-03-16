package com.ssafy.amagetdone.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ControllerServiceLombokConstructorTest {

	private static final Path MAIN_JAVA_PATH = Path.of("src", "main", "java", "com", "ssafy", "amagetdone");
	private static final Pattern FINAL_FIELD_PATTERN = Pattern.compile("private\\s+final\\s+[\\w$.<>?, ]+\\s+\\w+\\s*;");

	@Test
	void controllerAndServiceBeansUseLombokForConstructorInjection() throws IOException {
		List<Path> sourceFiles;
		try (var paths = Files.walk(MAIN_JAVA_PATH)) {
			sourceFiles = paths
				.filter(Files::isRegularFile)
				.filter(path -> path.toString().endsWith(".java"))
				.filter(path -> !path.toString().contains("global\\docs\\controller"))
				.toList();
		}

		for (Path sourceFile : sourceFiles) {
			String source = Files.readString(sourceFile);
			if (!isTargetBean(source) || !FINAL_FIELD_PATTERN.matcher(source).find()) {
				continue;
			}

			String simpleClassName = sourceFile.getFileName().toString().replace(".java", "");
			assertTrue(
				source.contains("@RequiredArgsConstructor"),
				() -> simpleClassName + " must declare @RequiredArgsConstructor for constructor injection"
			);
			assertFalse(
				source.contains("public " + simpleClassName + "("),
				() -> simpleClassName + " should not keep a manual constructor"
			);
		}
	}

	private boolean isTargetBean(String source) {
		return source.contains("@RestController") || source.contains("@Service");
	}
}
