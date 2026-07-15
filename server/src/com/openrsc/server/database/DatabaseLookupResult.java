package com.openrsc.server.database;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Classifies a database lookup without exposing database exception messages.
 *
 * <p>Lookup callers supply the database method's existing success predicate,
 * such as non-null for a record or a non-negative player ID. Database failures
 * retain only privacy-safe diagnostic metadata; raw exception messages can
 * contain query values and must not be included in player-facing or server-log
 * output.</p>
 */
public final class DatabaseLookupResult<T> {
	public enum Status {
		FOUND,
		NOT_FOUND,
		FAILED
	}

	@FunctionalInterface
	public interface Lookup<T> {
		T execute() throws GameDatabaseException;
	}

	private static final String DATABASE_PACKAGE_PREFIX = "com.openrsc.server.database.";

	private final Status status;
	private final T value;
	private final String failureType;
	private final String failureOrigin;

	private DatabaseLookupResult(final Status status, final T value,
			final String failureType, final String failureOrigin) {
		this.status = status;
		this.value = value;
		this.failureType = failureType;
		this.failureOrigin = failureOrigin;
	}

	public static <T> DatabaseLookupResult<T> resolve(final Lookup<T> lookup,
			final Predicate<T> isFound) {
		Objects.requireNonNull(lookup, "lookup");
		Objects.requireNonNull(isFound, "isFound");

		try {
			final T value = lookup.execute();
			if (isFound.test(value)) {
				return new DatabaseLookupResult<>(Status.FOUND, value, null, null);
			}
			return new DatabaseLookupResult<>(Status.NOT_FOUND, null, null, null);
		} catch (final GameDatabaseException failure) {
			return new DatabaseLookupResult<>(Status.FAILED, null,
				failure.getClass().getSimpleName(), findFailureOrigin(failure));
		}
	}

	public boolean isFound() {
		return status == Status.FOUND;
	}

	public boolean isNotFound() {
		return status == Status.NOT_FOUND;
	}

	public boolean isFailure() {
		return status == Status.FAILED;
	}

	public T getValue() {
		if (!isFound()) {
			throw new IllegalStateException("Database lookup did not return a value");
		}
		return value;
	}

	public String getFailureType() {
		if (!isFailure()) {
			throw new IllegalStateException("Database lookup did not fail");
		}
		return failureType;
	}

	public String getFailureOrigin() {
		if (!isFailure()) {
			throw new IllegalStateException("Database lookup did not fail");
		}
		return failureOrigin;
	}

	private static String findFailureOrigin(final GameDatabaseException failure) {
		for (final StackTraceElement frame : failure.getStackTrace()) {
			final String className = frame.getClassName();
			if (className.startsWith(DATABASE_PACKAGE_PREFIX)
					&& !className.equals(GameDatabaseException.class.getName())
					&& !className.equals(DatabaseLookupResult.class.getName())) {
				return className + "#" + frame.getMethodName() + ":" + frame.getLineNumber();
			}
		}
		return "database-layer";
	}
}
