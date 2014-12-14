package oakbot.command.javadoc;

import java.util.Collection;

/**
 * Thrown if multiple classes were found.
 * @see JavadocDao#getClassInfo(String)
 * @author Michael Angstadt
 */
public class MultipleClassesFoundException extends RuntimeException{
	private static final long serialVersionUID = -6218458106841347985L;
	private final Collection<String> classes;
	
	public MultipleClassesFoundException(Collection<String> classes){
		this.classes = classes;
	}
	
	public Collection<String> getClasses(){
		return classes;
	}
	
	@Override
    public String toString(){
		return classes.toString();
	}
}