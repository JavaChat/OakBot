package oakbot.command.javadoc;

/**
 * Contains information on a method parameter.
 * @author Michael Angstadt
 */
public record ParameterInfo(ClassName type, String name, boolean array, boolean varargs, String generic) {
}
