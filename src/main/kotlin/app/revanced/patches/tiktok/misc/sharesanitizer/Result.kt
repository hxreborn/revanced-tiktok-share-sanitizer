package app.revanced.patches.tiktok.misc.sharesanitizer

sealed class Result<out T, out E> {
    data class Ok<out T>(val value: T) : Result<T, Nothing>()
    data class Err<out E>(val error: E) : Result<Nothing, E>()

    inline fun isOk(): Boolean = this is Ok
    inline fun isErr(): Boolean = this is Err

    inline fun getOrNull(): T? = when (this) {
        is Ok -> value
        is Err -> null
    }

    inline fun errorOrNull(): E? = when (this) {
        is Ok -> null
        is Err -> error
    }

    inline fun getOrThrow(): T = when (this) {
        is Ok -> value
        is Err -> throw ResultException("Result is Err: $error", error)
    }

    /**
     * Exception thrown by getOrThrow() when Result is Err.
     */
    class ResultException(message: String, val error: Any?) : RuntimeException(message)

    inline fun getOrElse(default: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Ok -> value
        is Err -> default
    }

    inline fun <U> map(transform: (T) -> U): Result<U, E> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> Err(error)
    }

    inline fun <F> mapErr(transform: (E) -> F): Result<T, F> = when (this) {
        is Ok -> Ok(value)
        is Err -> Err(transform(error))
    }

    inline fun <U> andThen(transform: (T) -> Result<U, @UnsafeVariance E>): Result<U, E> = when (this) {
        is Ok -> transform(value)
        is Err -> Err(error)
    }

    inline fun <U> fold(
        onSuccess: (T) -> U,
        onFailure: (E) -> U
    ): U = when (this) {
        is Ok -> onSuccess(value)
        is Err -> onFailure(error)
    }
}

inline fun <T> ok(value: T): Result<T, Nothing> = Result.Ok(value)
inline fun <E> err(error: E): Result<Nothing, E> = Result.Err(error)
