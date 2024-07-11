package s2.bdd.data.parser

import org.springframework.context.annotation.Configuration

@Configuration
class StringParser: EntryParser<String>(
    output = String::class,
    parseErrorMessage = "",
    parser = { it }
)

@Configuration
class IntParser: EntryParser<Int>(
    output = Int::class,
    parseErrorMessage = "Expected Int value",
    parser = { it.toIntOrNull() }
)

@Configuration
class LongParser: EntryParser<Long>(
    output = Long::class,
    parseErrorMessage = "Expected Long value",
    parser = { it.toLongOrNull() }
)

@Configuration
class FloatParser: EntryParser<Float>(
    output = Float::class,
    parseErrorMessage = "Expected Float value",
    parser = { it.toFloatOrNull() }
)

@Configuration
class DoubleParser: EntryParser<Double>(
    output = Double::class,
    parseErrorMessage = "Expected Double value",
    parser = { it.toDoubleOrNull() }
)

@Configuration
class BooleanParser: EntryParser<Boolean>(
    output = Boolean::class,
    parseErrorMessage = """Expected "true" or "false" """,
    parser = { it.toBooleanStrictOrNull() }
)
