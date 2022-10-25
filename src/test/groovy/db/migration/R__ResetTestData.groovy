package db.migration

import org.apache.commons.lang3.RandomUtils
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

import java.sql.Connection

class R__ResetTestData extends BaseJavaMigration {

    @Override
    Integer getChecksum() {
        return RandomUtils.nextInt()
    }

    @Override
    void migrate(Context context) throws Exception {
        context.connection.with { con ->
            truncateAll(con)
        }
    }

    private static final List<String> TABLES = []

    private static def truncateAll(Connection con) {
        if (TABLES.isEmpty()) {
            con.getMetaData().getTables(con.getCatalog(), null, null, 'TABLE').withCloseable { rs ->
                while (rs.next()) {
                    def schema = rs.getString('TABLE_SCHEM')
                    def tableName = rs.getString('TABLE_NAME')
                    if (schema != 'public' && !tableName.startsWith('flyway_')) {
                        TABLES.add("$schema.${ tableName}")
                    }
                }
            }
        }
        con.createStatement().withCloseable { st ->
            st.execute("TRUNCATE ${TABLES.join(', ')}")
        }
    }
}
