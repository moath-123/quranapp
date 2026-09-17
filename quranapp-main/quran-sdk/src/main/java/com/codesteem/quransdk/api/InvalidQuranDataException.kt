package com.codesteem.quransdk.api

/**
 * Thrown by [QuranApi.initialize] when the bundled Quran data is incomplete or inconsistent.
 * Nothing is imported in that case, so the previous data (if any) stays intact.
 */
class InvalidQuranDataException(message: String) : IllegalStateException(message)
