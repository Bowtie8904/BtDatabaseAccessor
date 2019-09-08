package core.db;

import bt.db.EmbeddedDatabase;
import bt.db.config.DatabaseConfiguration;
import bt.db.constants.SqlType;
import bt.db.constants.SqlValue;
import bt.db.statement.clause.Column;

/**
 * @author &#8904
 *
 */
public class RecentDatabase extends EmbeddedDatabase
{
    public RecentDatabase(DatabaseConfiguration config)
    {
        super(config);
    }

    /**
     * @see bt.db.DatabaseAccess#createTables()
     */
    @Override
    protected void createTables()
    {
        create().table("recent_database")
                .column(new Column("database_path", SqlType.VARCHAR).size(2000).primaryKey()
                                                                    .comment("The path of the database folder."))
                .column(new Column("timestamp", SqlType.TIMESTAMP).defaultValue(SqlValue.SYSTIMESTAMP)
                                                                  .comment("The time when this database was last used by this application."))
                .onAlreadyExists((statement, e) ->
                {
                    return 0;
                })
                .commit()
                .execute(true);
    }
}