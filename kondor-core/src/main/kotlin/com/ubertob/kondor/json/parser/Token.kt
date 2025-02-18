package com.ubertob.kondor.json.parser


sealed interface Token {
    interface StringToken : Token {
        val text: String
    }

    interface NumberToken : Token {
        fun toNumber(): Number
    }

    interface PunctuationToken : Token {
        val char: Char
    }

    interface BooleanToken : Token {
        val value: Boolean
    }

    interface NullToken : Token
} 