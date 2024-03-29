/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.index;

import org.h2.result.SearchRow;
import org.h2.table.TableFilter;

/**
 * A spatial index. Spatial indexes are used to speed up searching spatial/geometric data.
 */
public interface SpatialIndex extends Index {

    /**
     * Find a row or a list of rows and create a cursor to iterate over the result.
     *
     * @param filter the table filter (which possibly knows
     *          about additional conditions)
     * @param intersection the geometry which values should intersect with, or null for anything
     * @return the cursor to iterate over the results
     */
    Cursor findByGeometry(TableFilter filter, SearchRow intersection);
}
