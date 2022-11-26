package org.stormpx.animed

import kotlinx.serialization.Required
import kotlinx.serialization.json.JsonElement

@kotlinx.serialization.Serializable
data class JsonRpcRequest(
    @Required
    val jsonrpc: String = "2.0",
    val method : String,
    val id: String,
    val params: Array<JsonElement>,
)

@kotlinx.serialization.Serializable
class JsonRpcError<E>(
    val code: Int,
    val message: String,
    val data: E?=null,
)

@kotlinx.serialization.Serializable
data class JsonRpcResult<T,E>(
    val jsonrpc: String,
    val id: String,
    val result: T? =null,
    val error: JsonRpcError<E>? =null,
)



