package args

import java.lang.IllegalArgumentException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ArgumentParser private constructor(
    private val flags: List<String>,
    val unnamedArguments: List<String>,
    private val argumentMap: Map<String, List<String>>
) {
    //region Convenience Methods

    //Bools

    fun hasFlag(name: String): Boolean {
        return flags.contains(name)
    }

    fun getBoolean(name: String): Boolean? {
        return argumentMap[name]?.firstOrNull()?.toBoolean()
    }

    //Ints

    fun getInt(name: String): Int? {
        return argumentMap[name]?.firstOrNull()?.toInt()
    }

    //Doubles

    fun getDouble(name: String): Double? {
        return argumentMap[name]?.firstOrNull()?.toDouble()
    }

    //Strings

    fun getString(name: String): String? {
        return argumentMap[name]?.firstOrNull()
    }

    fun getStringList(name: String): List<String>? {
        return argumentMap[name]
    }

    //endregion Convenience Methods

    companion object {
        fun parse(
            options: List<ArgumentOption>,
            argumentValues: List<String>,
            allowUnnamedArgs: Boolean = true,
            ignoreCase: Boolean = true
        ): ArgumentParser? {
            var pos = 0

            val argumentMap = HashMap<String, List<String>>()
            val unnamedArguments = ArrayList<String>()

            while (pos < argumentValues.size) {
                val currentValue = argumentValues[pos]
                val argument = options.firstOrNull { arg ->
                    arg.markers.any { flag ->
                        val normalizedFlag = if (ignoreCase) flag.toLowerCase() else flag
                        val compareValue = if (ignoreCase) currentValue.toLowerCase() else currentValue

                        normalizedFlag == compareValue
                    }
                }

                if (argument == null) {
                    if (allowUnnamedArgs) {
                        unnamedArguments.add(currentValue)
                    } else {
                        throw IllegalArgumentException("Unexpected argument $currentValue")
                    }
                } else {
                    val isRoom = (pos + argument.inputCount) < argumentValues.size
                    if (!isRoom) {
                        throw IllegalArgumentException("$currentValue expecting ${argument.inputCount} inputs")
                    } else {
                        val takeStart = pos + 1
                        val takeStop = takeStart + argument.inputCount
                        val values = argumentValues.slice(takeStop..takeStop)
                        argumentMap[argument.name] = values

                        pos += argument.inputCount

                    }
                }

                pos++
            }

            //Add required with defaults that are missing
            options.forEach { option ->
                val missing = !argumentMap.containsKey(option.name)
                if (missing && option.required && option.defaultValues.size == option.inputCount) {
                    argumentMap[option.name] = option.defaultValues
                }
            }

            val missingArgs = options.filter { option ->
                option.required && !argumentMap.containsKey(option.name)
            }

            return if (missingArgs.isNotEmpty()) {
                println("The following arguments were expected:")
                missingArgs.forEach { arg ->
                    println("    ${arg.name} (${arg.markers.joinToString()})")
                }
                null
            } else {
                val flags = argumentMap.toList().filter { (_, values) -> values.isEmpty() }.map { (key, _) -> key }
                val prunedArgs = argumentMap.filter { (_, values) -> values.isNotEmpty() }
                return ArgumentParser(flags, unnamedArguments, prunedArgs)
            }
        }


    }

}