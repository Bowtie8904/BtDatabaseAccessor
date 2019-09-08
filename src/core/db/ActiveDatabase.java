package core.db;

import bt.db.EmbeddedDatabase;
import bt.db.config.DatabaseConfiguration;

/**
 * @author &#8904
 *
 */
public class ActiveDatabase extends EmbeddedDatabase
{
    public ActiveDatabase(DatabaseConfiguration config)
    {
        super(config);
    }

    /**
     * @see bt.db.DatabaseAccess#createTables()
     */
    @Override
    protected void createTables()
    {
    }

    @Override
    protected void createDefaultProcedures()
    {
    }
}