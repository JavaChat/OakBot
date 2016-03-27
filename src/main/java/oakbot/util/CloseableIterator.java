package oakbot.util;

import java.io.Closeable;
import java.util.Iterator;

/**
 * An iterator that is closeable.
 * @author Michael Angstadt
 * @param <T> the type of element returned by the iterator
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {
	//empty
}
