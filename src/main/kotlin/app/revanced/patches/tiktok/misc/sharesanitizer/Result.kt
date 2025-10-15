package app.revanced.patches.tiktok.misc.sharesanitizer

sealed class Result<out T, out E> {
    data class Ok<out T>(val value: T) : Result<T, Nothing>()
    data class Err<out E>(val error: E) : Result<Nothing, E>()

    fun isOk(): Boolean = this is Ok
    fun isErr(): Boolean = this is Err

    fun getOrNull(): T? = when (this) {
        is Ok -> value
        is Err -> null
    }

    fun errorOrNull(): E? = when (this) {
        is Ok -> null
        is Err -> error
    }

    fun getOrThrow(): T = when (this) {
        is Ok -> value
        is Err -> throw ResultException("Result is Err: $error", error)
    }

    /**
     * Exception thrown by getOrThrow() when Result is Err.
     */
    class ResultException(message: String, val error: Any?) : RuntimeException(message)

    fun getOrElse(default: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Ok -> value
        is Err -> default
    }

    fun <U> map(transform: (T) -> U): Result<U, E> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> Err(error)
    }

    fun <F> mapErr(transform: (E) -> F): Result<T, F> = when (this) {
        is Ok -> Ok(value)
        is Err -> Err(transform(error))
    }

    fun <U> andThen(transform: (T) -> Result<U, @UnsafeVariance E>): Result<U, E> = when (this) {
        is Ok -> transform(value)
        is Err -> Err(error)
    }
}

fun <T> ok(value: T): Result<T, Nothing> = Result.Ok(value)
fun <E> err(error: E): Result<Nothing, E> = Result.Err(error)
