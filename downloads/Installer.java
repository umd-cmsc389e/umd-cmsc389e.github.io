import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class Installer {
	private static Path download(String spec) throws IOException {
		var path = download(spec, Paths.get(""));
		path.toFile().deleteOnExit();
		return path;
	}

	private static Path download(String spec, Path path) throws IOException {
		path = Files.createDirectories(path).resolve(spec.substring(spec.lastIndexOf('/') + 1));
		if (Files.notExists(path))
			try (var in = new URL("https://" + spec).openStream()) {
				Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
			}
		return path;
	}

	public static void main(String[] args) throws Exception {
		String command;
		String installer;
		Path minecraft;

		// Detect OS and set system-specific options
		switch (System.getProperty("os.name").split(" ")[0]) {
		case "Mac":
			command = "open %s";
			installer = "Minecraft.dmg";
			minecraft = Paths.get(System.getenv("user.home"), "Library", "Application Support", "minecraft");
			break;
		case "Windows":
			command = "msiexec /i %s /passive";
			installer = "MinecraftInstaller.msi";
			minecraft = Paths.get(System.getenv("APPDATA"), ".minecraft");
			break;
		default:
			command = "tar xfzv %s";
			installer = "Minecraft.tar.gz";
			minecraft = Paths.get(System.getenv("user.home"), ".minecraft");
		}

		// Download and install Minecraft
		var builder = new ProcessBuilder(
				String.format(command, download("launcher.mojang.com/download/" + installer)).split(" ")).inheritIO();
		if (builder.start().waitFor() == 0) {
			// Download the mod
			download("github.com/CMSC-389E/mod-and-testing-framework/releases/latest/download/circuitry.jar",
					minecraft.resolve("mods"));

			// Display instructions
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			if (JOptionPane.showConfirmDialog(null,
					"Press OK in the next window to install Minecraft Forge. Do not change the listed directory.",
					"Minecraft Forge", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
				// Create minimal launcher_profiles.json
				Files.write(minecraft.resolve("launcher_profiles.json"), "{}".getBytes());

				// Download and install Minecraft Forge
				var pattern = Pattern.compile("\"1\\.15\\.2-recommended\": \"([\\d.]+)\"");
				try (var in = new BufferedReader(new InputStreamReader(
						new URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/promotions_slim.json")
								.openStream()))) {
					builder.command("java", "-jar", download(String.format(
							"files.minecraftforge.net/maven/net/minecraftforge/forge/1.15.2-%1$s/forge-1.15.2-%1$s-installer.jar",
							in.lines().parallel().map(pattern::matcher).filter(Matcher::find).findFirst().get()
									.group(1))).toString())
							.start().waitFor();
				}
			}
		}
	}
}