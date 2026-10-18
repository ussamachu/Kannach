package com.example

import java.util.Locale

enum class PluralForm {
    ZERO,
    ONE,
    TWO,
    FEW,
    MANY,
    OTHER
}

fun plural(lang: String, n: Int): PluralForm {
    val code = lang.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
    val count = kotlin.math.abs(n)
    return when (code) {
        "ar" -> when {
            count == 0 -> PluralForm.ZERO
            count == 1 -> PluralForm.ONE
            count == 2 -> PluralForm.TWO
            count in 3..10 || (count > 10 && count % 100 in 3..10) -> PluralForm.FEW
            count in 11..99 || (count > 99 && count % 100 in 11..99) -> PluralForm.MANY
            else -> PluralForm.OTHER
        }
        "fr" -> if (count == 0 || count == 1) PluralForm.ONE else PluralForm.OTHER
        else -> if (count == 1) PluralForm.ONE else PluralForm.OTHER
    }
}

data class PluralString(
    val forms: Map<PluralForm, String>
) {
    constructor(vararg pairs: Pair<PluralForm, String>) : this(pairs.toMap())

    constructor(
        one: String,
        other: String,
        zero: String? = null,
        two: String? = null,
        few: String? = null,
        many: String? = null
    ) : this(
        buildMap {
            if (zero != null) put(PluralForm.ZERO, zero)
            put(PluralForm.ONE, one)
            if (two != null) put(PluralForm.TWO, two)
            if (few != null) put(PluralForm.FEW, few)
            if (many != null) put(PluralForm.MANY, many)
            put(PluralForm.OTHER, other)
        }
    )

    operator fun get(form: PluralForm): String? = forms[form]

    fun resolve(lang: String, n: Int): String {
        val form = plural(lang, n)
        return forms[form]
            ?: forms[PluralForm.OTHER]
            ?: forms[PluralForm.ONE]
            ?: forms.values.firstOrNull()
            ?: ""
    }

    fun format(lang: String, n: Int, vararg args: Pair<String, String>): String {
        val formattedN = I18n.formatNumber(n, lang)
        var result = resolve(lang, n).replace("{n}", formattedN)
        for ((key, value) in args) {
            val token = if (key.startsWith("{") && key.endsWith("}")) key else "{$key}"
            result = result.replace(token, value)
        }
        return I18n.applyNumerals(result, lang)
    }

    fun contains(sub: CharSequence): Boolean = forms.values.any { it.contains(sub) }

    fun isNotBlank(): Boolean = forms.isNotEmpty() && forms.values.all { it.isNotBlank() }
}
