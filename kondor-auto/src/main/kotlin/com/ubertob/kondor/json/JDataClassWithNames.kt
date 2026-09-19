package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

/**
 * Marker annotation placed on the base converter to indicate that
 * constructor binding should be performed by parameter NAME using
 * Kotlin reflection, rather than by position.
 *
 * No annotations are required on the target data class nor on
 * the concrete converters extending this base class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConstructorByName

/**
 * A JDataClass variant that does NOT require the registered properties
 * to be in the same order as the data class constructor parameters.
 *
 * It binds constructor arguments by NAME using Kotlin reflection and
 * supports:
 * - nullable parameters: pass explicit null if missing
 * - defaulted parameters: omitted from callBy so Kotlin uses defaults
 * - strict checking for missing non-null, non-optional parameters
 *
 * No annotations are needed on the data class or on concrete converters; this
 * base class carries a marker annotation to make the intention explicit.
 */
@ConstructorByName
abstract class JDataClassWithNames<T : Any>(private val kClazz: KClass<T>) : JObj<T>() {

    private val kConstructor: KFunction<T> by lazy {
        (kClazz.primaryConstructor ?: kClazz.constructors.first()) as KFunction<T>
    }

    override fun FieldsValues.deserializeOrThrow(path: NodePath): T =
        buildInstance(getMap(), path).orThrow()

    fun buildConsParamsMap(args: ObjectFields, path: NodePath): Map<KParameter, Any?> {
        val params = kConstructor.parameters

        val callArgs = mutableMapOf<KParameter, Any?>()
        for (p in params) {
            val name = p.name
            if (args.containsKey(name)) {
                callArgs[p] = args[name]
            } else if (p.isOptional) {
                // omit so callBy will use default value
            } else if (p.type.isMarkedNullable) {
                // explicitly pass null for missing nullable
                callArgs[p] = null
            } else {
                error("Missing required parameter '$name' for constructor ${kClazz} at $path")
            }
        }
        return callArgs
    }

    fun buildInstance(args: ObjectFields, path: NodePath): JsonOutcome<T> = try {

        val consParams = buildConsParamsMap(args, path)
        val instance: T = kConstructor.callBy(consParams)
        instance.asSuccess()
    } catch (t: Exception) {
        ConverterJsonError(
            path,
            "Error calling constructor ${kClazz} by name using params $args. Error: ${t.message}"
        ).asFailure()
    }
}
