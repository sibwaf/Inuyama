package sibwaf.norm

import java.sql.Connection
import java.sql.PreparedStatement

class JdbcQueryRunner(private val connectionProvider: () -> Connection) : QueryRunner {

    private fun prepareStatement(query: RawQuery): PreparedStatement {
        val connection = connectionProvider()

        val statement = connection.prepareStatement(query.sql)
        for ((index, parameter) in query.parameters.withIndex()) {
            statement.setObject(index + 1, parameter)
        }

        return statement
    }

    override fun executeQuery(query: RawQuery): Iterable<QueryResultRow> {
        val statement = prepareStatement(query)
        val result = statement.executeQuery()

        val columns = result.metaData.columnCount

        return sequence {
            while (result.next()) {
                val indices = 1..columns
                yield(QueryResultRow(indices.map { result.getObject(it) }))
            }
        }.asIterable()
    }

    override fun executeUpdate(query: RawQuery): Int {
        val statement = prepareStatement(query)
        return statement.executeUpdate()
    }
}