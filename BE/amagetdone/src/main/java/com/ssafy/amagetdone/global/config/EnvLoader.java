package com.ssafy.amagetdone.global.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.github.cdimascio.dotenv.Dotenv;

public final class EnvLoader {

	private EnvLoader() {
	}

	public static void loadRequiredEnv(Path baseDirectory) {
		Path envDirectory = findEnvDirectory(baseDirectory.toAbsolutePath());
		if (envDirectory == null) {
			throw new IllegalStateException(".env file is required at " + baseDirectory.toAbsolutePath().resolve(".env"));
		}

		Dotenv dotenv = Dotenv.configure()
			.directory(envDirectory.toString())
			.filename(".env")
			.load();

		dotenv.entries().forEach(entry -> {
			System.setProperty(entry.getKey(), entry.getValue());
			if ("SPRING_PROFILES_ACTIVE".equals(entry.getKey())) {
				System.setProperty("spring.profiles.active", entry.getValue());
			}
		});
	}

	private static Path findEnvDirectory(Path directory) {
		Path current = directory;
		while (current != null) {
			if (Files.exists(current.resolve(".env"))) {
				return current;
			}
			current = current.getParent();
		}
		return findEnvDirectoryInMainWorktree(directory).orElse(null);
	}

	private static Optional<Path> findEnvDirectoryInMainWorktree(Path directory) {
		try {
			Path worktreeRoot = runGitCommand(directory, "rev-parse", "--show-toplevel")
				.map(Path::of)
				.orElse(null);
			if (worktreeRoot == null) {
				return Optional.empty();
			}

			List<String> worktreeList = runGitCommand(directory, "worktree", "list", "--porcelain")
				.map(output -> List.of(output.split("\\R")))
				.orElse(List.of());
			if (worktreeList.isEmpty()) {
				return Optional.empty();
			}

			Path mainWorktreeRoot = worktreeList.stream()
				.filter(line -> line.startsWith("worktree "))
				.map(line -> Path.of(line.substring("worktree ".length()).trim()))
				.findFirst()
				.orElse(null);
			if (mainWorktreeRoot == null || mainWorktreeRoot.equals(worktreeRoot)) {
				return Optional.empty();
			}

			Path relativePath = worktreeRoot.relativize(directory);
			Path candidateDirectory = mainWorktreeRoot.resolve(relativePath);
			if (Files.exists(candidateDirectory.resolve(".env"))) {
				return Optional.of(candidateDirectory);
			}
			return Optional.empty();
		} catch (Exception exception) {
			return Optional.empty();
		}
	}

	private static Optional<String> runGitCommand(Path directory, String... commandParts) throws Exception {
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(directory.toFile());
		String[] command = new String[commandParts.length + 1];
		command[0] = "git";
		System.arraycopy(commandParts, 0, command, 1, commandParts.length);
		processBuilder.command(command);
		Process process = processBuilder.start();
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			return Optional.empty();
		}
		String output = new String(process.getInputStream().readAllBytes()).trim();
		if (output.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(output);
	}
}
