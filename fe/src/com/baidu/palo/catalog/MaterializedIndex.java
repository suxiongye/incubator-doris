// Modifications copyright (C) 2017, Baidu.com, Inc.
// Copyright 2017 The Apache Software Foundation

// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.baidu.palo.catalog;

import com.baidu.palo.common.io.Text;
import com.baidu.palo.common.io.Writable;

import com.google.common.collect.Lists;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The OlapTraditional table is a materialized table which stored as rowcolumnar file or columnar file
 */
public class MaterializedIndex extends MetaObject implements Writable {
    public enum IndexState {
        NORMAL,
        ROLLUP,
        SCHEMA_CHANGE
    }

    private long id;

    private IndexState state;
    private long rowCount;

    private Map<Long, Tablet> idToTablets;
    // this is for keeping tablet order
    private List<Tablet> tablets;

    // for push after rollup index finished
    private long rollupIndexId;
    private long rollupFinishedVersion;

    public MaterializedIndex() {
        this.state = IndexState.NORMAL;
        this.idToTablets = new HashMap<Long, Tablet>();
        this.tablets = new ArrayList<Tablet>();
    }

    public MaterializedIndex(long id, IndexState state) {
        this.id = id;

        this.state = state;
        if (this.state == null) {
            this.state = IndexState.NORMAL;
        }

        this.idToTablets = new HashMap<Long, Tablet>();
        this.tablets = new ArrayList<Tablet>();

        this.rowCount = 0;

        this.rollupIndexId = -1L;
        this.rollupFinishedVersion = -1L;
    }

    public List<Tablet> getTablets() {
        return tablets;
    }

    public List<Long> getTabletIdsInOrder() {
        List<Long> tabletIds = Lists.newArrayList();
        for (Tablet tablet : tablets) {
            tabletIds.add(tablet.getId());
        }
        return tabletIds;
    }

    public Tablet getTablet(long tabletId) {
        return idToTablets.get(tabletId);
    }

    public void clearTabletsForRestore() {
        idToTablets.clear();
        tablets.clear();
    }

    public void addTablet(Tablet tablet, TabletMeta tabletMeta) {
        addTablet(tablet, tabletMeta, false);
    }

    public void addTablet(Tablet tablet, TabletMeta tabletMeta, boolean isRestore) {
        idToTablets.put(tablet.getId(), tablet);
        tablets.add(tablet);
        if (!isRestore) {
            Catalog.getCurrentInvertedIndex().addTablet(tablet.getId(), tabletMeta);
        }
    }

    public void setIdForRestore(long idxId) {
        this.id = idxId;
    }

    public long getId() {
        return id;
    }

    public void setState(IndexState state) {
        this.state = state;
    }

    public IndexState getState() {
        return this.state;
    }

    public long getRowCount() {
        return rowCount;
    }

    public void setRowCount(long rowCount) {
        this.rowCount = rowCount;
    }

    public void setRollupIndexInfo(long rollupIndexId, long rollupFinishedVersion) {
        this.rollupIndexId = rollupIndexId;
        this.rollupFinishedVersion = rollupFinishedVersion;
    }

    public long getRollupIndexId() {
        return rollupIndexId;
    }

    public long getRollupFinishedVersion() {
        return rollupFinishedVersion;
    }

    public void clearRollupIndexInfo() {
        this.rollupIndexId = -1L;
        this.rollupFinishedVersion = -1L;
    }

    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeLong(id);

        Text.writeString(out, state.name());
        out.writeLong(rowCount);

        int tabletCount = tablets.size();
        out.writeInt(tabletCount);
        for (Tablet tablet : tablets) {
            tablet.write(out);
        }

        out.writeLong(rollupIndexId);
        out.writeLong(rollupFinishedVersion);
    }

    public void readFields(DataInput in) throws IOException {
        super.readFields(in);

        id = in.readLong();

        state = IndexState.valueOf(Text.readString(in));
        rowCount = in.readLong();

        int tabletCount = in.readInt();
        for (int i = 0; i < tabletCount; ++i) {
            Tablet tablet = Tablet.read(in);
            tablets.add(tablet);
            idToTablets.put(tablet.getId(), tablet);
        }

        rollupIndexId = in.readLong();
        rollupFinishedVersion = in.readLong();
    }

    public static MaterializedIndex read(DataInput in) throws IOException {
        MaterializedIndex materializedIndex = new MaterializedIndex();
        materializedIndex.readFields(in);
        return materializedIndex;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MaterializedIndex)) {
            return false;
        }

        MaterializedIndex table = (MaterializedIndex) obj;

        // Check idToTablets
        if (table.idToTablets == null) {
            return false;
        }
        if (idToTablets.size() != table.idToTablets.size()) {
            return false;
        }
        for (Entry<Long, Tablet> entry : idToTablets.entrySet()) {
            long key = entry.getKey();
            if (!table.idToTablets.containsKey(key)) {
                return false;
            }
            if (!entry.getValue().equals(table.idToTablets.get(key))) {
                return false;
            }
        }

        return (state.equals(table.state))
                && (rowCount == table.rowCount);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("index id: ").append(id).append("; ");
        buffer.append("index state: ").append(state.name()).append("; ");

        buffer.append("row count: ").append(rowCount).append("; ");
        buffer.append("tablets size: ").append(tablets.size()).append("; ");
        //
        buffer.append("tablets: [");
        for (Tablet tablet : tablets) {
            buffer.append("tablet: ").append(tablet.toString()).append(", ");
        }
        buffer.append("]; ");

        buffer.append("rollup index id: ").append(rollupIndexId).append("; ");
        buffer.append("rollup finished version: ").append(rollupFinishedVersion).append("; ");

        return buffer.toString();
    }
}
