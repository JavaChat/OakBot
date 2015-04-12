package oakbot.doclet.cli;

/**
 * Represents a library that is hosted in a Maven repository.
 * @author Michael Angstadt
 */
public class MavenLibrary {
	private final String groupId;
	final String artifactId;
	final String version;

	public MavenLibrary(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	public static MavenLibrary parse(String id) {
		String split[] = id.split(":");
		if (split.length != 3) {
			throw new IllegalArgumentException("Must be in the format: \"groupId:artifactId:version\"");
		}
		return new MavenLibrary(split[0], split[1], split[2]);
	}

	private String getBaseUrl() {
		String group = groupId.replace('.', '/');
		return "http://repo1.maven.org/maven2/" + group + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version;
	}

	public String getJarUrl() {
		return getBaseUrl() + ".jar";
	}

	public String getSourcesUrl() {
		return getBaseUrl() + "-sources.jar";
	}

	public String getPomUrl() {
		return getBaseUrl() + ".pom";
	}

	@Override
	public String toString() {
		return groupId + ":" + artifactId + ":" + version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		MavenLibrary other = (MavenLibrary) obj;
		if (artifactId == null) {
			if (other.artifactId != null) return false;
		} else if (!artifactId.equals(other.artifactId)) return false;
		if (groupId == null) {
			if (other.groupId != null) return false;
		} else if (!groupId.equals(other.groupId)) return false;
		if (version == null) {
			if (other.version != null) return false;
		} else if (!version.equals(other.version)) return false;
		return true;
	}
}