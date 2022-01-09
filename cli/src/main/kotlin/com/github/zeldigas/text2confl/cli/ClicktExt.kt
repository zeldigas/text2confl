package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.groups.ParameterGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parsers.FlagOptionParser
import com.github.ajalt.clikt.parsers.OptionParser
import com.github.ajalt.clikt.sources.ValueSource
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun parameterMissing(what: String, cliOption: String, fileOption: String): Nothing {
    throw PrintMessage("$what is not specified. Use `$cliOption` option or `$fileOption` in config file", error = true)
}

// Code below is almost 1-to-1 copy from clikt sources because of no way to have nullable flag
// and a lot of internal code. Remove after https://github.com/ajalt/clikt/issues/329 is fixed

fun RawOption.optionalFlag(vararg secondaryNames:String): CustomFlagOption<Boolean?> {
    return CustomFlagOption(
        names = names,
        secondaryNames = secondaryNames.toSet(),
        optionHelp = optionHelp,
        hidden = hidden,
        helpTags = helpTags,
        valueSourceKey = valueSourceKey,
        envvar = envvar,
        transformEnvvar = {
            when (it.lowercase()) {
                "true", "t", "1", "yes", "y", "on" -> true
                "false", "f", "0", "no", "n", "off" -> false
                else -> throw BadParameterValue(context.localization.boolConversionError(it), this)
            }
        },
        transformAll = {
            if (it.isEmpty()) null else it.last() !in secondaryNames
        },
        validator = {}
    )
}

class CustomFlagOption<T> internal constructor(
    names: Set<String>,
    override val secondaryNames: Set<String>,
    override val optionHelp: String,
    override val hidden: Boolean,
    override val helpTags: Map<String, String>,
    override val valueSourceKey: String?,
    val envvar: String?,
    val transformEnvvar: OptionTransformContext.(String) -> T,
    val transformAll: CallsTransformer<String, T>,
    val validator: OptionValidator<T>,
) : OptionDelegate<T> {
    override var parameterGroup: ParameterGroup? = null
    override var groupName: String? = null
    override fun metavar(context: Context): String? = null
    override val nvalues: Int get() = 0
    override val parser = FlagOptionParser
    override var value: T by NullableLateinit("Cannot read from option delegate before parsing command line")
        private set
    override var names: Set<String> = names
        private set

    override operator fun provideDelegate(
        thisRef: ParameterHolder,
        prop: KProperty<*>,
    ): ReadOnlyProperty<ParameterHolder, T> {
        names = inferOptionNames(names, prop.name)
        thisRef.registerOption(this)
        return this
    }

    override fun finalize(context: Context, invocations: List<OptionParser.Invocation>) {
        val transformContext = OptionTransformContext(this, context)
        value = when (val v = getFinalValue(context, invocations, envvar)) {
            is FinalValue.Parsed -> transformAll(transformContext, invocations.map { it.name })
            is FinalValue.Sourced -> {
                if (v.values.size != 1 || v.values[0].values.size != 1) {
                    val message = context.localization.invalidFlagValueInFile(longestName() ?: "")
                    throw UsageError(message, this)
                }
                transformEnvvar(transformContext, v.values[0].values[0])
            }
            is FinalValue.Envvar -> transformEnvvar(transformContext, v.value)
        }
    }

    override fun postValidate(context: Context) {
        validator(OptionTransformContext(this, context), value)
    }
}

internal fun Option.getFinalValue(
    context: Context,
    invocations: List<OptionParser.Invocation>,
    envvar: String?,
): FinalValue {
    return when {
        invocations.isNotEmpty() -> FinalValue.Parsed(invocations)
        context.readEnvvarBeforeValueSource -> {
            readEnvVar(context, envvar) ?: readValueSource(context)
        }
        else -> {
            readValueSource(context) ?: readEnvVar(context, envvar)
        }
    } ?: FinalValue.Parsed(emptyList())
}

private fun Option.readValueSource(context: Context): FinalValue? {
    return context.valueSource?.getValues(context, this)?.ifEmpty { null }
        ?.let { FinalValue.Sourced(it) }
}

private fun Option.readEnvVar(context: Context, envvar: String?): FinalValue? {
    val env = inferEnvvar(names, envvar, context.autoEnvvarPrefix) ?: return null
    return context.readEnvvar(env)?.let { FinalValue.Envvar(env, it) }
}

internal fun inferEnvvar(names: Set<String>, envvar: String?, autoEnvvarPrefix: String?): String? {
    if (envvar != null) return envvar
    if (names.isEmpty() || autoEnvvarPrefix == null) return null
    val name = splitOptionPrefix(names.maxByOrNull { it.length }!!).second
    if (name.isEmpty()) return null
    return autoEnvvarPrefix + "_" + name.replace(Regex("\\W"), "_").uppercase()
}

/** Split an option token into a pair of prefix to simple name. */
internal fun splitOptionPrefix(name: String): Pair<String, String> =
    when {
        name.length < 2 || name[0].isLetterOrDigit() -> "" to name
        name.length > 2 && name[0] == name[1] -> name.slice(0..1) to name.substring(2)
        else -> name.substring(0, 1) to name.substring(1)
    }

internal sealed class FinalValue {
    data class Parsed(val values: List<OptionParser.Invocation>) : FinalValue()
    data class Sourced(val values: List<ValueSource.Invocation>) : FinalValue()
    data class Envvar(val key: String, val value: String) : FinalValue()
}

internal class NullableLateinit<T>(private val errorMessage: String) : ReadWriteProperty<Any, T> {
    private object UNINITIALIZED

    private var value: Any? = UNINITIALIZED

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (value === UNINITIALIZED) throw IllegalStateException(errorMessage)

        try {
            @Suppress("UNCHECKED_CAST")
            return value as T
        } catch (e: ClassCastException) {
            throw IllegalStateException(errorMessage)
        }
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }
}

internal fun Option.longestName(): String? = names.maxByOrNull { it.length }


internal fun inferOptionNames(names: Set<String>, propertyName: String): Set<String> {
    if (names.isNotEmpty()) {
        val invalidName = names.find { !it.matches(Regex("""[\-@/+]{1,2}[\w\-_]+""")) }
        require(invalidName == null) { "Invalid option name \"$invalidName\"" }
        return names
    }
    val normalizedName = "--" + propertyName.replace(Regex("""[a-z][A-Z]""")) {
        "${it.value[0]}-${it.value[1]}"
    }.lowercase()
    return setOf(normalizedName)
}