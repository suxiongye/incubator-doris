// Copyright (c) 2017, Baidu.com, Inc. All Rights Reserved

// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.baidu.palo.task;

import com.baidu.palo.thrift.TDropTabletReq;
import com.baidu.palo.thrift.TTaskType;

public class DropReplicaTask extends AgentTask {
    private int schemaHash; // set -1L as unknown

    public DropReplicaTask(long backendId, long tabletId, int schemaHash) {
        super(null, backendId, tabletId, TTaskType.DROP, -1L, -1L, -1L, -1L, tabletId);
        this.schemaHash = schemaHash;
    }
    
    public TDropTabletReq toThrift() {
        TDropTabletReq request = new TDropTabletReq(tabletId);
        if (this.schemaHash != -1) {
            request.setSchema_hash(schemaHash);
        }
        return request;
    }

    public int getSchemaHash() {
        return schemaHash;
    }
}
