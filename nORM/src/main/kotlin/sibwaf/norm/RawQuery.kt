package sibwaf.norm

class RawQuery(val sql: String) {

    private val mutableParameters = mutableListOf<Any?>()
    val parameters = mutableParameters as List<Any?>

    fun addParameter(parameter: Any?) {
        mutableParameters += parameter
    }
}

interface QueryRunner {

    fun executeQuery(query: RawQuery): Iterable<QueryResultRow>
    fun executeQuery(sql: String) = executeQuery(RawQuery(sql))

    fun executeUpdate(query: RawQuery): Int
    fun executeUpdate(sql: String) = executeUpdate(RawQuery(sql))
}

class QueryResultRow(val data: List<Any?>)