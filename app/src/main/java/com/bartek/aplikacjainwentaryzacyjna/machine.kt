package com.bartek.aplikacjainwentaryzacyjna

data class Machine(
    val id: String,
    val name: String,
    val category: String = "",
    val status: String = ""
)
