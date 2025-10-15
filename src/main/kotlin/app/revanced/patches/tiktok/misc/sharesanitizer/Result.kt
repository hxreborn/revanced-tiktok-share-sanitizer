package app.revanced.patches.tiktok.misc.sharesanitizer

/**
 * Represents the result of an operation that can fail.
 *
 * Inspired by Rust's Result<T, E> type for explicit error handling.
 */
sealed class Result<out T, out E> {
    /**
     * Success case containing a value of type T.
     */
    data class Ok<out T>(val value: T) : Result<T, Nothing>()

    /**
     * Failure case containing an error of type E.
     */
    data class Err<out E>(val error: E) : Result<Nothing, E>()

    /**
     * Returns true if this is an Ok result.
     */
    fun isOk(): Boolean = this is Ok

    /**
     * Returns true if this is an Err result.
     */
    fun isErr(): Boolean = this is Err

    /**
     * Returns the value if Ok, or null if Err.
     */
    fun getOrNull(): T? = when (this) {
        is Ok -> value
        is Err -> null
    }

    /**
     * Returns the error if Err, or null if Ok.
     */
    fun errorOrNull(): E? = when (this) {
        is Ok -> null
        is Err -> error
    }

    /**
     * Returns the value if Ok, or throws the error if Err (when E is Throwable).
     */
    fun getOrThrow(): T = when (this) {
        is Ok -> value
        is Err -> throw RuntimeException("Result is Err: $error")
    }

    /**
     * Returns the value if Ok, or the provided default if Err.
     */
    fun getOrElse(default: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Ok -> value
        is Err -> default
    }

    /**
     * Maps the Ok value using the provided function.
     */
    fun <U> map(transform: (T) -> U): Result<U, E> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> Err(error)
    }

    /**
     * Maps the Err value using the provided function.
     */
    fun <F> mapErr(transform: (E) -> F): Result<T, F> = when (this) {
        is Ok -> Ok(value)
        is Err -> Err(transform(error))
    }

    /**
     * Chains another Result-returning operation if this is Ok.
     */
    fun <U> andThen(transform: (T) -> Result<U, @UnsafeVariance E>): Result<U, E> = when (this) {
        is Ok -> transform(value)
        is Err -> Err(error)
    }
}

/**
 * Helper function to create an Ok result.
 */
fun <T> ok(value: T): Result<T, Nothing> = Result.Ok(value)

/**
 * Helper function to create an Err result.
 */
fun <E> err(error: E): Result<Nothing, E> = Result.Err(error)
