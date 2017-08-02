package oakbot.chat.event;

import java.time.LocalDateTime;

/**
 * Represents a chat room event.
 * @author Michael Angstadt
 */
public abstract class Event {
	protected final long eventId;
	protected final LocalDateTime timestamp;

	protected Event(Builder<?, ?> builder) {
		timestamp = builder.timestamp;
		eventId = builder.eventId;
	}

	/**
	 * Gets the time the event occurred.
	 * @return the timestamp
	 */
	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	/**
	 * Gets the unique ID of the individual event.
	 * @return the event ID
	 */
	public long getEventId() {
		return eventId;
	}

	/**
	 * Used for constructing {@link Event} instances.
	 * @author Michael Angstadt
	 * @param <T> the event class that is being built
	 * @param <U> allows the setter methods to return instances of the
	 * implementing child class
	 */
	public static abstract class Builder<T extends Event, U extends Builder<?, ?>> {
		protected LocalDateTime timestamp;
		protected long eventId;

		@SuppressWarnings("unchecked")
		private final U this_ = (U) this;

		protected Builder() {
			//empty
		}

		protected Builder(Event original) {
			timestamp = original.timestamp;
			eventId = original.eventId;
		}

		/**
		 * Sets the time the event occurred.
		 * @param timestamp the timestamp
		 * @return this
		 */
		public U timestamp(LocalDateTime timestamp) {
			this.timestamp = timestamp;
			return this_;
		}

		/**
		 * Sets the unique ID of the individual event.
		 * @param eventId the event ID
		 * @return this
		 */
		public U eventId(long eventId) {
			this.eventId = eventId;
			return this_;
		}

		/**
		 * Builds the event object.
		 * @return the event object
		 */
		public abstract T build();
	}
}
