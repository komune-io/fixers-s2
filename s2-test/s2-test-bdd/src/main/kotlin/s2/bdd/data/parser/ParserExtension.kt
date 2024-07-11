package s2.bdd.data.parser

inline fun <reified R: Any> Map<String, String>.extract(key: String): R? {
    return EntryParserDirectory.select(R::class).singleOrNull(this, key)
}

inline fun <reified R: Any> Map<String, String>.safeExtract(key: String): R {
    return EntryParserDirectory.select(R::class).single(this, key)
}

inline fun <reified R: Any> Map<String, String>.extractList(key: String): List<R>? {
    return EntryParserDirectory.select(R::class).listOrNull(this, key)
}

inline fun <reified R: Any> Map<String, String>.safeExtractList(key: String): List<R> {
    return EntryParserDirectory.select(R::class).list(this, key)
}
