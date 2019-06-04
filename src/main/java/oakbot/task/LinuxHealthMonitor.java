package oakbot.task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Monitors the health of a Linux server.
 * @author Michael Angstadt
 */
public class LinuxHealthMonitor extends HealthMonitor {
	private final String aptCheckPath;

	/**
	 * @param roomIds the rooms to post the messages to
	 * @param aptCheckPath the file system path to the "apt-check" command
	 */
	public LinuxHealthMonitor(List<Integer> roomIds, String aptCheckPath) {
		super(roomIds);
		this.aptCheckPath = aptCheckPath;
	}

	@Override
	public int getNumSecurityUpdates() throws Exception {
		/*
		 * The apt-check command returns output in the following format:
		 * 
		 * NUM_TOTAL_UPDATES;NUM_SECURITY_UPDATES
		 * 
		 * For example, the output "128;68" means there are 128 updates, 68 of
		 * which are considered to be security updates.
		 */

		/*
		 * For some reason, the command output is sent to the error stream, so
		 * redirect all error output to the standard stream.
		 */
		Process process = new ProcessBuilder(aptCheckPath).redirectErrorStream(true).start();

		String line;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			line = reader.readLine();
		}

		String split[] = line.split(";");
		return Integer.parseInt(split[1]);
	}
}
