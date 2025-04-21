package com.example.calculuscalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculuscalculator.ui.theme.CalculusCalculatorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextOverflow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculusCalculatorTheme {
                val result = remember { mutableStateOf("") }
                val animateExpression = remember {mutableStateOf(false)}
                val animateResult = remember {mutableStateOf(false)}
                val calculator = remember { CalculusCalculator(result,animateExpression,animateResult) }
                val coroutineScope = rememberCoroutineScope()
                ResponsiveLayout(calculator, result, coroutineScope)
            }
        }
    }
    @Composable
    fun ResponsiveLayout(
        calculator: CalculusCalculator,
        result: MutableState<String>,
        coroutineScope: CoroutineScope,
    ) {
        val isComplexMode = remember { mutableStateOf(false) }
        val columns = if (!isComplexMode.value) 4 else 8

        val keys = calculator.getAllKeys()


        Calculator(calculator, result, columns,isComplexMode, keys, coroutineScope)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun Calculator(
        calculator: CalculusCalculator,
        result: MutableState<String>,
        columns: Int,
        isComplexMode: MutableState<Boolean>,
        keys: List<CalculusCalculator.Key>,
        coroutineScope: CoroutineScope
    ) {
        val expression by calculator.expression
        val context = LocalContext.current
        val isLoading = remember { mutableStateOf(false) }
        val filteredKeys = remember(keys, isComplexMode) {
            if (!isComplexMode.value) {
                val indexOfEquals = keys.indexOfFirst { it.label == "=" }
                if (indexOfEquals != -1) keys.take(indexOfEquals+1) else keys
            } else {
                keys
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top= 20.dp,end = 8.dp, start= 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { isComplexMode.value = !isComplexMode.value },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text(if (isComplexMode.value) "Simple Mode" else "Complex Mode")
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .border(
                        width = 2.dp,
                        color = Color.Blue,
                        shape = RoundedCornerShape(8.dp)
                    ).padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (calculator.animateExpression.value) {
                    AnimatedContent(
                        targetState = expression,
                        transitionSpec = {
                            (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                        },
                        label = "ExpressionAnimation"
                    ) { targetExpression ->
                        Text(
                            targetExpression,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.End),
                            fontSize = if (targetExpression.length > 5) 75.sp else 90.sp,
                        )
                    }

                    // Reset animation flag
                    LaunchedEffect(expression) {
                        calculator.animateExpression.value = false
                    }
                } else {
                    Text(
                        calculator.formatExpression(expression),
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.End),
                        fontSize = if (expression.length > 5) 60.sp else 75.sp,
                        overflow = TextOverflow.Clip
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp), // Reserve space even when empty
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (isLoading.value) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp),
                            color = Color.Gray,
                            strokeWidth = 3.dp
                        )
                    } else {
                        if (calculator.animateResult.value) {
                            AnimatedContent(
                                targetState = calculator.formatExpression(result.value),
                                transitionSpec = {
                                    (fadeIn() + slideInHorizontally()).togetherWith(fadeOut() + slideOutHorizontally())
                                },
                                label = "ResultAnimation"
                            ) { animatedResult ->
                                Text(
                                    calculator.formatExpression(animatedResult),
                                    color = Color.LightGray,
                                    fontSize = if (animatedResult.length > 10) 35.sp else 40.sp
                                )
                            }

                            // Reset animation flag
                            LaunchedEffect(result.value) {
                                calculator.animateResult.value = false
                            }
                        } else {
                            Text(
                                calculator.formatExpression(result.value),
                                color = Color.LightGray,
                                fontSize = if (result.value.length > 10) 35.sp else 40.sp
                            )
                        }

                    }
                }

            }
            Spacer(modifier = Modifier.height(40.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .padding(bottom = 50.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.Bottom,
                maxItemsInEachRow = columns,
            ) {
                filteredKeys.forEach { key ->
                    val buttonModifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                        .height(if (!isComplexMode.value) 60.dp else 40.dp)
                        .weight(1f)

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading.value = true
                                if (key.label == "∫"){
                                    calculator.evaluateExpression(result,"integrate", context)
                                }else if (key.label == "d/dx"){
                                    calculator.evaluateExpression(result, "differentiate", context)
                                }else {
                                    key.onClick()
                                }
                                isLoading.value = false
                            }

                        },
                        border = BorderStroke(1.dp, Color.Blue),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.LightGray
                        ),
                        modifier = buttonModifier
                    ) {
                        Text(key.label, fontSize = if (isComplexMode.value) 10.sp else 15.sp)
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun LayoutPreview() {
        CalculusCalculatorTheme {
            val result = remember { mutableStateOf("8") }
            val animateExpression = remember { mutableStateOf(false) }
            val animateResult = remember { mutableStateOf(false) }
            val calculator = remember { CalculusCalculator(result,animateExpression,animateResult).apply { setExpression("5+3") } }
            val coroutineScope = rememberCoroutineScope()
            val isComplexMode = remember { mutableStateOf(false)}

            Calculator(calculator, result, 4,isComplexMode, calculator.getAllKeys(), coroutineScope)
        }
    }
}
