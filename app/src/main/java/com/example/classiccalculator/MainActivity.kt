package com.example.classiccalculator

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class MainActivity : Activity() {
    private val bg = Color.rgb(10, 16, 24)
    private val key = Color.rgb(28, 39, 52)
    private val orange = Color.rgb(245, 130, 18)
    private val blue = Color.rgb(16, 105, 224)
    private val white = Color.rgb(241, 245, 249)
    private val muted = Color.rgb(160, 174, 192)
    private lateinit var expressionView: TextView
    private lateinit var resultView: TextView
    private lateinit var historyView: TextView
    private var expression = ""
    private var justEvaluated = false
    private var memory = BigDecimal.ZERO
    private var lastResult = BigDecimal.ZERO
    private val history = mutableListOf<String>()
    private val mc = MathContext(34, RoundingMode.HALF_EVEN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        refresh()
    }

    private fun rounded(color: Int, radius: Float = 18f): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius; setStroke(1, Color.rgb(65, 78, 92)) }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(bg)
        }
        val header = TextView(this).apply {
            text = "الآلة الحاسبة"
            textSize = 23f; setTextColor(white); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL; setPadding(8, 6, 8, 10)
        }
        root.addView(header, LinearLayout.LayoutParams(-1, 52))

        val display = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setPadding(18, 14, 18, 14)
            background = rounded(Color.rgb(8, 13, 19), 22f)
        }
        expressionView = TextView(this).apply {
            textSize = 20f; setTextColor(muted); gravity = Gravity.END
            maxLines = 2
        }
        resultView = TextView(this).apply {
            textSize = 42f; setTextColor(white); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END; maxLines = 1
        }
        display.addView(expressionView, LinearLayout.LayoutParams(-1, 46))
        display.addView(resultView, LinearLayout.LayoutParams(-1, 60))
        root.addView(display, LinearLayout.LayoutParams(-1, 136))

        historyView = TextView(this).apply {
            text = "◷   التاريخ"
            textSize = 15f; setTextColor(muted); setPadding(14, 8, 14, 8)
            background = rounded(Color.rgb(14, 22, 31), 16f)
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener { showHistory() }
        }
        val hp = LinearLayout.LayoutParams(-1, 44); hp.topMargin = 10; hp.bottomMargin = 8
        root.addView(historyView, hp)

        val rows = listOf(
            listOf("MC","MR","M+","M−","MS"),
            listOf("%","±","√","x²","1/x"),
            listOf("7","8","9","÷"),
            listOf("4","5","6","×"),
            listOf("1","2","3","−"),
            listOf("C","0",".","+","="),
        )
        rows.forEachIndexed { ri, row ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.forEach { label ->
                val b = TextView(this).apply {
                    text = label; textSize = if (ri == 0) 18f else 25f
                    setTextColor(white); gravity = Gravity.CENTER
                    typeface = if (label in listOf("+","−","×","÷","=")) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    background = rounded(when {
                        label == "=" -> blue
                        label in listOf("+","−","×","÷") -> orange
                        ri == 0 || ri == 1 || label == "C" -> Color.rgb(35, 47, 61)
                        else -> key
                    }, 18f)
                    isClickable = true; isFocusable = true
                    setOnClickListener { press(label) }
                }
                val lp = LinearLayout.LayoutParams(0, 0, if (ri == 5 && label == "0") 2f else 1f)
                lp.setMargins(3, 3, 3, 3)
                lp.height = if (ri == 0) 48 else if (ri == 1) 50 else 0
                if (ri >= 2) lp.height = 0
                // Weight by row so the keypad adapts to available screen height.
                if (ri >= 2) { lp.height = 0; lp.weight = 1f }
                line.addView(b, lp)
            }
            val rowParams = LinearLayout.LayoutParams(-1, if (ri < 2) 52 else 0, if (ri < 2) 0f else 1f)
            rowParams.topMargin = 1; rowParams.bottomMargin = 1
            root.addView(line, rowParams)
        }
        setContentView(root)
    }

    private fun press(s: String) {
        try {
            when (s) {
                "C" -> { expression = ""; justEvaluated = false }
                "=" -> calculate()
                "±" -> toggleSign()
                "√" -> unary { sqrtDecimal(it) }
                "x²" -> unary { it.multiply(it, mc) }
                "1/x" -> unary { if (it.signum() == 0) throw ArithmeticException("القسمة على صفر") else BigDecimal.ONE.divide(it, mc) }
                "%" -> unary { it.divide(BigDecimal("100"), mc) }
                "⌫" -> { if (expression.isNotEmpty()) expression = expression.dropLast(1) }
                "MC" -> memory = BigDecimal.ZERO
                "MR" -> { expression += format(memory); justEvaluated = false }
                "M+" -> memory = memory.add(currentValue(), mc)
                "M−" -> memory = memory.subtract(currentValue(), mc)
                "MS" -> memory = currentValue()
                else -> {
                    if (s.firstOrNull()?.isDigit() == true || s == ".") {
                        if (justEvaluated) expression = ""
                        justEvaluated = false
                        if (s == "." && currentNumberHasDot()) return
                        expression += s
                    } else if (s in listOf("+","−","×","÷")) {
                        justEvaluated = false
                        if (expression.isNotEmpty() || s == "−") expression += when(s) { "×" -> "*"; "÷" -> "/"; "−" -> "-"; else -> "+" }
                    }
                }
            }
            refresh()
        } catch (e: Exception) {
            resultView.text = "خطأ"
            expressionView.text = e.message ?: "عملية غير صالحة"
        }
    }


    private fun sqrtDecimal(value: BigDecimal): BigDecimal {
        if (value.signum() < 0) throw ArithmeticException("عدد سالب")
        if (value.signum() == 0) return BigDecimal.ZERO
        var guess = BigDecimal.valueOf(kotlin.math.sqrt(value.toDouble()))
        if (guess.signum() == 0) guess = BigDecimal.ONE
        repeat(80) {
            val next = guess.add(value.divide(guess, mc)).divide(BigDecimal("2"), mc)
            if (next.compareTo(guess) == 0) return next
            guess = next
        }
        return guess
    }

    private fun currentNumberHasDot(): Boolean {
        val tail = expression.takeLastWhile { it.isDigit() || it == '.' }
        return tail.contains('.')
    }
    private fun toggleSign() {
        val v = currentValue()
        expression = format(v.negate())
        justEvaluated = false
    }
    private fun unary(f: (BigDecimal) -> BigDecimal) {
        expression = format(f(currentValue()))
        justEvaluated = false
    }
    private fun currentValue(): BigDecimal = if (expression.isBlank()) BigDecimal.ZERO else Parser(expression, mc).parse()
    private fun calculate() {
        if (expression.isBlank()) return
        val source = expression
        val value = Parser(source, mc).parse()
        lastResult = value
        history.add(0, "$source = ${format(value)}")
        if (history.size > 30) history.removeAt(history.lastIndex)
        expression = format(value)
        justEvaluated = true
    }
    private fun refresh() {
        expressionView.text = expression.replace("*","×").replace("/","÷").replace("-","−").ifBlank { "أدخل العملية الحسابية" }
        resultView.text = if (expression.isBlank()) "0" else try { format(currentValue()) } catch (_: Exception) { "…" }
        historyView.text = "◷   التاريخ" + if (history.isNotEmpty()) "  •  ${history.size}" else ""
    }
    private fun format(v: BigDecimal): String {
        if (v.compareTo(BigDecimal.ZERO) == 0) return "0"
        return v.stripTrailingZeros().toPlainString()
    }
    private fun showHistory() {
        if (history.isEmpty()) {
            Toast.makeText(this, "لا توجد عمليات سابقة", Toast.LENGTH_SHORT).show(); return
        }
        val list = ListView(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, history)
        list.adapter = adapter
        val dialog = android.app.AlertDialog.Builder(this).setTitle("سجل العمليات").setView(list)
            .setNegativeButton("إغلاق", null).create()
        list.setOnItemClickListener { _, _, position, _ ->
            expression = history[position].substringAfter(" = ")
            justEvaluated = true; refresh(); dialog.dismiss()
        }
        dialog.show()
    }

    private class Parser(private val s: String, private val mc: MathContext) {
        private var p = 0
        fun parse(): BigDecimal {
            val result = expression()
            skip()
            if (p != s.length) throw IllegalArgumentException("صيغة غير صحيحة")
            return result
        }
        private fun expression(): BigDecimal {
            var x = term()
            while (true) {
                skip()
                if (eat('+')) x = x.add(term(), mc)
                else if (eat('-')) x = x.subtract(term(), mc)
                else return x
            }
        }
        private fun term(): BigDecimal {
            var x = factor()
            while (true) {
                skip()
                if (eat('*')) x = x.multiply(factor(), mc)
                else if (eat('/')) {
                    val y = factor()
                    if (y.signum() == 0) throw ArithmeticException("القسمة على صفر")
                    x = x.divide(y, mc)
                } else return x
            }
        }
        private fun factor(): BigDecimal {
            skip()
            if (eat('+')) return factor()
            if (eat('-')) return factor().negate()
            if (eat('(')) {
                val x = expression()
                skip()
                if (!eat(')')) throw IllegalArgumentException("قوس غير مغلق")
                return x
            }
            val start = p
            var dots = 0
            while (p < s.length && (s[p].isDigit() || s[p] == '.')) {
                if (s[p] == '.') dots++
                if (dots > 1) throw IllegalArgumentException("رقم عشري غير صحيح")
                p++
            }
            if (start == p || s.substring(start, p) == ".") throw IllegalArgumentException("أدخل رقماً صحيحاً")
            return BigDecimal(s.substring(start, p))
        }
        private fun skip() { while (p < s.length && s[p].isWhitespace()) p++ }
        private fun eat(c: Char): Boolean { if (p < s.length && s[p] == c) { p++; return true }; return false }
    }
}
