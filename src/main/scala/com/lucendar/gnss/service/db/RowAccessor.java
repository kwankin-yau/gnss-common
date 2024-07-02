/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */

package com.lucendar.gnss.service.db;

import com.lucendar.common.utils.DateTimeUtils;
import de.bytefish.pgbulkinsert.row.SimpleRow;

import java.time.Instant;
import java.time.ZonedDateTime;

public class RowAccessor {
    private final SimpleRow row;
    private int index;

    public RowAccessor(SimpleRow row) {
        this.row = row;
    }

    public SimpleRow getRow() {
        return row;
    }

    public int getIndex() {
        return index;
    }

    public void boo(Boolean value) {
        row.setBoolean(index, value);
        index++;
    }

    public void str(String value) {
        row.setText(index, value);
        index++;
    }

    public void int16(Integer value) {
        if (value != null)
            row.setShort(index, value.shortValue());
        else
            row.setShort(index, null);
        index++;
    }

    public void int32(Integer value) {
        row.setInteger(index, value);
        index++;
    }

    public void big(Long value) {
        row.setLong(index, value);
        index++;
    }

    public void big(String value) {
        if (value != null)
            big(Long.parseLong(value));
        else {
            row.setLong(index, null);
            index++;
        }
    }


    public void fld(Float value) {
        row.setFloat(index, value);
        index++;
    }

    public void dbl(Double value) {
        row.setDouble(index, value);
        index++;
    }

    public void tsz(Long value) {
        if (value != null) {
            row.setTimeStampTz(
                    index,
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(value), DateTimeUtils.ZONE_OFFSET_BEIJING)
            );
        } else
            row.setTimeStampTz(index, null);

        index++;
    }
}
