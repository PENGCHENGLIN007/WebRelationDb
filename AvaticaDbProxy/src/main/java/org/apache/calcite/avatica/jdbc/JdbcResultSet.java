/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.avatica.jdbc;

import com.google.common.base.Optional;
import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.apache.calcite.avatica.AvaticaStatement;
import org.apache.calcite.avatica.AvaticaUtils;
import org.apache.calcite.avatica.ColumnMetaData;
import org.apache.calcite.avatica.ColumnMetaData.ArrayType;
import org.apache.calcite.avatica.ColumnMetaData.AvaticaType;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.SqlType;
import org.apache.calcite.avatica.util.DateTimeUtils;

/** Implementation of {@link org.apache.calcite.avatica.Meta.MetaResultSet}
 * upon a JDBC {@link java.sql.ResultSet}.
 *
 * @see org.apache.calcite.avatica.jdbc.JdbcMeta */
class JdbcResultSet extends Meta.MetaResultSet {
    protected JdbcResultSet(String connectionId, int statementId,
            boolean ownStatement, Meta.Signature signature, Meta.Frame firstFrame) {
        this(connectionId, statementId, ownStatement, signature, firstFrame, -1L);
    }

    protected JdbcResultSet(String connectionId, int statementId,
            boolean ownStatement, Meta.Signature signature, Meta.Frame firstFrame,
            long updateCount) {
        super(connectionId, statementId, ownStatement, signature, firstFrame, updateCount);
    }

    /** Creates a result set. */
    public static JdbcResultSet create(String connectionId, int statementId,
            ResultSet resultSet) {
        // -1 still limits to 100 but -2 does not limit to any number
        return create(connectionId, statementId, resultSet,
                JdbcMeta.UNLIMITED_COUNT);
    }

    /** Creates a result set with maxRowCount.
     *
     * <p>If {@code maxRowCount} is -2 ({@link JdbcMeta#UNLIMITED_COUNT}),
     * returns an unlimited number of rows in a single frame; any other
     * negative value (typically -1) returns an unlimited number of rows
     * in frames of the default frame size. */
    public static JdbcResultSet create(String connectionId, int statementId,
            ResultSet resultSet, int maxRowCount) {
        try {
            Meta.Signature sig = JdbcMeta.signature(resultSet.getMetaData());
            return create(connectionId, statementId, resultSet, maxRowCount, sig);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static JdbcResultSet create(String connectionId, int statementId,
            ResultSet resultSet, int maxRowCount, Meta.Signature signature) {
        try {
            final Calendar calendar = DateTimeUtils.calendar();
            final int fetchRowCount;
            if (maxRowCount == JdbcMeta.UNLIMITED_COUNT) {
                fetchRowCount = -1;
            } else if (maxRowCount < 0L) {
                fetchRowCount = AvaticaStatement.DEFAULT_FETCH_SIZE;
            } else if (maxRowCount > AvaticaStatement.DEFAULT_FETCH_SIZE) {
                fetchRowCount = AvaticaStatement.DEFAULT_FETCH_SIZE;
            } else {
                fetchRowCount = maxRowCount;
            }
            final Meta.Frame firstFrame = frame(null, resultSet, 0, fetchRowCount, calendar,
                    Optional.of(signature));
            if (firstFrame.done) {
                resultSet.close();
            }
            return new JdbcResultSet(connectionId, statementId, true, signature,
                    firstFrame);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Creates a empty result set with empty frame */
    public static JdbcResultSet empty(String connectionId, int statementId,
            Meta.Signature signature) {
        return new JdbcResultSet(connectionId, statementId, true, signature,
                Meta.Frame.EMPTY);
    }

    /** Creates a result set that only has an update count. */
    public static JdbcResultSet count(String connectionId, int statementId,
            int updateCount) {
        return new JdbcResultSet(connectionId, statementId, true, null, null, updateCount);
    }

    /** Creates a frame containing a given number or unlimited number of rows
     * from a result set. */
    static Meta.Frame frame(StatementInfo info, ResultSet resultSet, long offset,
            int fetchMaxRowCount, Calendar calendar, Optional<Meta.Signature> sig) throws SQLException {
        final ResultSetMetaData metaData = resultSet.getMetaData();
        final int columnCount = metaData.getColumnCount();
        final int[] types = new int[columnCount];
        Set<Integer> arrayOffsets = new HashSet<>();
        for (int i = 0; i < types.length; i++) {
            types[i] = metaData.getColumnType(i + 1);
            if (Types.ARRAY == types[i]) {
                arrayOffsets.add(i);
            }
        }
        final List<Object> rows = new ArrayList<>();
        // Meta prepare/prepareAndExecute 0 return 0 row and done
        boolean done = fetchMaxRowCount == 0;
        for (int i = 0; fetchMaxRowCount < 0 || i < fetchMaxRowCount; i++) {
            final boolean hasRow;
            if (null != info) {
                hasRow = info.next();
            } else {
                hasRow = resultSet.next();
            }
            if (!hasRow) {
                done = true;
                resultSet.close();
                break;
            }
            Object[] columns = new Object[columnCount];
            for (int j = 0; j < columnCount; j++) {
                columns[j] = getValue(resultSet, types[j], j, calendar);
                if (arrayOffsets.contains(j)) {
                    // If we have an Array type, our Signature is lacking precision. We can't extract the
                    // component type of an Array from metadata, we have to update it as we're serializing
                    // the ResultSet.
                    final Array array = resultSet.getArray(j + 1);
                    // Only attempt to determine the component type for the array when non-null
                    if (null != array && sig.isPresent()) {
                        ColumnMetaData columnMetaData = sig.get().columns.get(j);
                        ArrayType arrayType = (ArrayType) columnMetaData.type;
                        SqlType componentSqlType = SqlType.valueOf(array.getBaseType());

                        // Avatica Server will always return non-primitives to ensure nullable is guaranteed.
                        ColumnMetaData.Rep rep = ColumnMetaData.Rep.serialRepOf(componentSqlType);
                        AvaticaType componentType = ColumnMetaData.scalar(array.getBaseType(),
                                array.getBaseTypeName(), rep);
                        // Update the ArrayType from the Signature
                        arrayType.updateComponentType(componentType);

                        // We only need to update the array's type once.
                        arrayOffsets.remove(j);
                    }
                }
            }
            rows.add(columns);
        }
        return new Meta.Frame(offset, done, rows);
    }

    private static Object getValue(ResultSet resultSet, int type, int j,
            Calendar calendar) throws SQLException {
        switch (type) {
            case Types.BIGINT:
                final long aLong = resultSet.getLong(j + 1);
                return aLong == 0 && resultSet.wasNull() ? null : aLong;
            case Types.INTEGER:
                final int anInt = resultSet.getInt(j + 1);
                return anInt == 0 && resultSet.wasNull() ? null : anInt;
            case Types.SMALLINT:
                final short aShort = resultSet.getShort(j + 1);
                return aShort == 0 && resultSet.wasNull() ? null : aShort;
            case Types.TINYINT:
                final byte aByte = resultSet.getByte(j + 1);
                return aByte == 0 && resultSet.wasNull() ? null : aByte;
            case Types.DOUBLE:
            case Types.FLOAT:
                final double aDouble = resultSet.getDouble(j + 1);
                return aDouble == 0D && resultSet.wasNull() ? null : aDouble;
            case Types.REAL:
                final float aFloat = resultSet.getFloat(j + 1);
                return aFloat == 0D && resultSet.wasNull() ? null : aFloat;
            case Types.DATE:
                final Date aDate = resultSet.getDate(j + 1, calendar);
                return aDate == null
                        ? null
                        : (int) (aDate.getTime() / DateTimeUtils.MILLIS_PER_DAY);
            case Types.TIME:
                final Time aTime = resultSet.getTime(j + 1, calendar);
                return aTime == null
                        ? null
                        : (int) (aTime.getTime() % DateTimeUtils.MILLIS_PER_DAY);
            case Types.TIMESTAMP:
                final Timestamp aTimestamp = resultSet.getTimestamp(j + 1, calendar);
                return aTimestamp == null ? null : aTimestamp.getTime();
            case Types.ARRAY:
                final Array array = resultSet.getArray(j + 1);
                if (null == array) {
                    return null;
                }
                try {
                    // Recursively extracts an Array using its ResultSet-representation
                    return extractUsingResultSet(array, calendar);
                } catch (UnsupportedOperationException | SQLFeatureNotSupportedException e) {
                    // Not every database might implement Array.getResultSet(). This call
                    // assumes a non-nested array (depends on the db if that's a valid assumption)
                    return extractUsingArray(array, calendar);
                }
            case Types.STRUCT:
                Struct struct = resultSet.getObject(j + 1, Struct.class);
                Object[] attrs = struct.getAttributes();
                List<Object> list = new ArrayList<>(attrs.length);
                for (Object o : attrs) {
                    list.add(o);
                }
                return list;
            default:
                return resultSet.getObject(j + 1);
        }
    }

    /**
     * Converts an Array into a List using {@link Array#getResultSet()}. This implementation is
     * recursive and can parse multi-dimensional arrays.
     */
    static List<?> extractUsingResultSet(Array array, Calendar calendar) throws SQLException {
        ResultSet arrayValues = array.getResultSet();
        TreeMap<Integer, Object> map = new TreeMap<>();
        while (arrayValues.next()) {
            // column 1 is the index in the array, column 2 is the value.
            // Recurse on `getValue` to unwrap nested types correctly.
            // `j` is zero-indexed and incremented for us, thus we have `1` being used twice.
            map.put(arrayValues.getInt(1), getValue(arrayValues, array.getBaseType(), 1, calendar));
        }
        // If the result set is not in the same order as the actual Array, TreeMap fixes that.
        // Need to make a concrete list to ensure Jackson serialization.
        return new ArrayList<>(map.values());
    }

    /**
     * Converts an Array into a List using {@link Array#getArray()}. This implementation assumes
     * a non-nested array. Use {link {@link #extractUsingResultSet(Array, Calendar)} if nested
     * arrays may be possible.
     */
    static List<?> extractUsingArray(Array array, Calendar calendar) throws SQLException {
        // No option but to guess as to what the type actually is...
        Object o = array.getArray();
        if (o instanceof List) {
            return (List<?>) o;
        }
        // Assume that it's a Java array.
        return AvaticaUtils.primitiveList(o);
    }
}

// End JdbcResultSet.java
