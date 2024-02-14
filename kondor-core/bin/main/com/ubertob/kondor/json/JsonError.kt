package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.getPath
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.OutcomeError


typealias JsonOutcome<T> = Outcome<JsonError, T>

sealed class JsonError() : OutcomeError {
    abstract val path: NodePath
    abstract val reason: String

    override fun toString(): String = msg
}


data class InvalidJsonError(override val path: NodePath, override val reason: String) : JsonError() {
    override val msg = "Error parsing node <${path.getPath()}> $reason"
}

data class ConverterJsonError(override val path: NodePath, override val reason: String) : JsonError() {
    override val msg = "Error converting node <${path.getPath()}> $reason"
}

data class JsonPropertyError(override val path: NodePath, val propertyName: String, override val reason: String) : JsonError() {
    override val msg = "Error reading property <$propertyName> of node <${path.getPath()}> $reason"
}