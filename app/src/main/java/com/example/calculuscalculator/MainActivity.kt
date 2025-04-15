package com.example.calculuscalculator

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculuscalculator.ui.theme.CalculusCalculatorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculusCalculatorTheme {
                val result = remember { mutableStateOf("") }
                val calculator = remember { CalculusCalculator(result) }
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
        val columns = if (isComplexMode.value) 4 else 8
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
        val isLoading = remember { mutableStateOf(false) }
        val filteredKeys = remember(keys, isComplexMode) {
            if (isComplexMode.value) {
                val indexOfEquals = keys.indexOfFirst { it.label == "=" }
                if (indexOfEquals != -1) keys.take(indexOfEquals+1) else keys
            } else {
                keys
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
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
                Text(
                    expression,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.End),
                    fontSize = 90.sp,
                )
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
                        Text(
                            calculator.formatExpression(result.value),
                            color = Color.LightGray,
                            fontSize = 40.sp
                        )
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
                        .height(if (isComplexMode) 60.dp else 40.dp)
                        .weight(1f)

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading.value = true
                                key.onClick()
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
                        Text(key.label)
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
            val calculator = remember { CalculusCalculator(result).apply { setExpression("5+3") } }
            val coroutineScope = rememberCoroutineScope()

            Calculator(calculator, result, 4,mutableStateOf(false), calculator.getAllKeys(), coroutineScope)
        }
    }
}
