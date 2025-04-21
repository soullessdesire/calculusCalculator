package com.example.calculuscalculator

import androidx.compose.runtime.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import net.objecthunter.exp4j.ExpressionBuilder
import okhttp3.*
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resumeWithException

class CalculusCalculator(val result: MutableState<String>, val animateExpression: MutableState<Boolean>, val animateResult: MutableState<Boolean>) {

    private val _expression = mutableStateOf("")
    val expression: State<String> get() = _expression

    private val client = OkHttpClient()
    private val stdUrl = "https://ad64-197-136-183-22.ngrok-free.app/"

    data class Key(
        val label: String,
        val onClick: suspend () -> Unit
    )

    fun appendToExpression(str: String) {
        _expression.value += str
        if (isSimpleArithmetic(_expression.value)) {
            val evaluated = evaluateLocally(_expression.value)
            if (evaluated != null) {
                result.value = evaluated
            }
        }
    }

    fun removeFromExpression() {
        _expression.value = _expression.value.dropLast(1)
    }

    fun clearExpression() {
        _expression.value = ""
        result.value = ""
    }

    fun getExpression(): String = _expression.value

    fun setExpression(str: String) {
        _expression.value = str
    }
    fun isSimpleArithmetic(expr: String): Boolean {
        return expr.matches(Regex("^[0-9+\\-*/().^ ]+\$"))
    }

    fun getAllKeys(): List<Key> {
        return listOf(
            Key("C") { clearExpression() },
            Key("DEL") { removeFromExpression() },
            Key("(") { appendToExpression("(") },
            Key(")") { appendToExpression(")") },

            Key("7") { appendToExpression("7") },
            Key("8") { appendToExpression("8") },
            Key("9") { appendToExpression("9") },
            Key("/") { appendToExpression("/") },

            Key("4") { appendToExpression("4") },
            Key("5") { appendToExpression("5") },
            Key("6") { appendToExpression("6") },
            Key("*") { appendToExpression("*") },

            Key("1") { appendToExpression("1") },
            Key("2") { appendToExpression("2") },
            Key("3") { appendToExpression("3") },
            Key("-") { appendToExpression("-") },

            Key("0") { appendToExpression("0") },
            Key(".") { appendToExpression(".") },
            Key("+") { appendToExpression("+") },
            Key("=") { evaluateExpression(result, "evaluate") },

            Key("^") { appendToExpression("^") },
            Key("exp") { appendToExpression("exp(") },
            Key("log") { appendToExpression("log10(") },

            Key("sin") { appendToExpression("sin(") },
            Key("cos") { appendToExpression("cos(") },
            Key("tan") { appendToExpression("tan(") },
            Key("sec") { appendToExpression("1/cos(") },

            Key("csc") { appendToExpression("1/sin(") },
            Key("cot") { appendToExpression("1/tan(") },
            Key("∫") { },
            Key("d/dx") { },
            Key("x") { appendToExpression("x")},

            Key("x²") { appendToExpression("^2") },
            Key("√") { appendToExpression("sqrt(") },
            Key("^3") { appendToExpression("^3") },
            Key("∛") { appendToExpression("^(1/3)") },
            Key("10^x") { appendToExpression("10^") },
            Key("x⁻¹") { appendToExpression("^(-1)") },
            Key("!") { appendToExpression("!") },
        )
    }

    suspend fun evaluateExpression(result: MutableState<String>, method: String) {
        val currentExpression = _expression.value

        if (method == "evaluate" && isSimpleArithmetic(currentExpression)) {
            try {
                val evaluated = ExpressionBuilder(currentExpression).build().evaluate()
                animateExpression.value = true
                animateResult.value = true
                _expression.value = evaluated.toString()
                result.value = ""
                return
            } catch (_: Exception) {
                // Fallback to server
            }
        }else if(method == "evaluate" && !isSimpleArithmetic(currentExpression)){
            val url = encodedURL(currentExpression, method)
            val request = Request.Builder().url(url).build()
            getResponse(result, request)
            return
        }

        val variable = "x"

        val url = encodedURL(currentExpression, method, variable)
        val request = Request.Builder().url(url).build()
        getResponse(result, request)
    }


    fun encodedURL(expression: String, method: String, variable: String = "x"): String {
        val encodedExpression = URLEncoder.encode(expression, StandardCharsets.UTF_8.toString())
        return if (method == "evaluate")
            "$stdUrl$method?expression=$encodedExpression"
        else
            "$stdUrl$method?expression=$encodedExpression&variable=${variable}"
    }

    suspend fun getResponse(result: MutableState<String>, request: Request) {
        try {
            val response = client.newCall(request).await()
            if (response.isSuccessful) {
                val unformattedExpression = response.body?.string() ?: ""
                result.value = formatExpression(unformattedExpression)
            } else {
                result.value = "Error: ${response.code}"
            }
        } catch (e: Exception) {
            result.value = "Network Error: ${e.message}"
        }
    }
    fun formatExpression(expression: String): String {
        val regex = Regex("""\^(-?\d+)""") // match ^2, ^-1, ^12, etc.

        return regex.replace(expression) { matchResult ->
            val exponent = matchResult.groupValues[1]
            exponent.map { toSuperscript(it) }.joinToString("")
        }
    }
    fun evaluateLocally(expression: String): String? {
        return try {
            val result = ExpressionBuilder(expression).build().evaluate()
            result.toString()
        } catch (_: Exception) {
            null // Return null if not evaluable
        }
    }

    fun toSuperscript(char: Char): Char {
        return when (char) {
            '0' -> '⁰'
            '1' -> '¹'
            '2' -> '²'
            '3' -> '³'
            '4' -> '⁴'
            '5' -> '⁵'
            '6' -> '⁶'
            '7' -> '⁷'
            '8' -> '⁸'
            '9' -> '⁹'
            '-' -> '⁻'
            else -> char
        }
    }
}
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { cont ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                cont.resume(response) { it.printStackTrace() }
            }

            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }
        })
    }
}