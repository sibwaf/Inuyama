package ru.sibwaf.inuyama.http

@DslMarker
annotation class SecurityConfigDsl

interface SecurityConfig {
    fun getStrategy(path: String): SecurityStrategy
}

interface RouteSecurityConfig {
    val strategy: SecurityStrategy
    fun matches(path: String): Boolean
}

@SecurityConfigDsl
interface NestableSecurityConfigBuilder {
    fun subroute(path: String, init: RouteSecurityConfigBuilder.() -> Unit)
}

@SecurityConfigDsl
interface RouteSecurityConfigBuilder : NestableSecurityConfigBuilder {
    var strategy: SecurityStrategy?
}

private class RouteSecurityConfigBuilderImpl : RouteSecurityConfigBuilder {

    override var strategy: SecurityStrategy? = null

    private val mappings: MutableList<Pair<String, RouteSecurityConfigBuilderImpl>> = mutableListOf()

    override fun subroute(path: String, init: RouteSecurityConfigBuilder.() -> Unit) {
        mappings += path to RouteSecurityConfigBuilderImpl().apply(init)
    }

    fun build(strategy: SecurityStrategy): List<RouteSecurityConfigImpl> {
        return mappings.flatMap { (path, subroute) ->
            subroute.build(this.strategy ?: strategy).map {
                RouteSecurityConfigImpl(
                    path = "$path${it.path}",
                    strategy = it.strategy
                )
            } + RouteSecurityConfigImpl(path, subroute.strategy ?: this.strategy ?: strategy)
        }
    }
}

private class SecurityConfigImpl(
    private val routes: List<RouteSecurityConfig>,
    private val defaultStrategy: SecurityStrategy
) : SecurityConfig {

    override fun getStrategy(path: String): SecurityStrategy {
        return routes.firstOrNull { it.matches(path) }?.strategy ?: defaultStrategy
    }
}

private class RouteSecurityConfigImpl(val path: String, override val strategy: SecurityStrategy) : RouteSecurityConfig {
    override fun matches(path: String): Boolean {
        return path.startsWith(this.path, ignoreCase = true)
    }
}

fun securityConfig(strategy: SecurityStrategy, init: NestableSecurityConfigBuilder.() -> Unit): SecurityConfig {
    return SecurityConfigImpl(
        routes = RouteSecurityConfigBuilderImpl().apply(init).build(strategy),
        defaultStrategy = strategy
    )
}
