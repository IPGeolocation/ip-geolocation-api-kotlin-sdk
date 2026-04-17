package io.ipgeolocation.sdk

enum class Language(val code: String) {
    EN("en"),
    DE("de"),
    RU("ru"),
    JA("ja"),
    FR("fr"),
    CN("cn"),
    ES("es"),
    CS("cs"),
    IT("it"),
    KO("ko"),
    FA("fa"),
    PT("pt");

    companion object {
        fun fromCode(code: String): Language? = entries.firstOrNull { it.code == code.lowercase() }
    }
}

