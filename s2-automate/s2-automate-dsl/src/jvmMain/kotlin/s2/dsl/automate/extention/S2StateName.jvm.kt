package s2.dsl.automate.extention

/** The JVM holds the enum itself, never a JSON copy of it. */
internal actual fun enumCopyName(value: Any): String? = null
