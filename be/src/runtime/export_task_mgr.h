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

#ifndef BDG_PALO_BE_SRC_RUNTIME_EXPORT_TASK_MGR_H
#define BDG_PALO_BE_SRC_RUNTIME_EXPORT_TASK_MGR_H

#include <mutex>
#include <vector>
#include <unordered_set>

#include "common/status.h"
#include "util/lru_cache.hpp"
#include "util/hash_util.hpp"
#include "gen_cpp/Types_types.h"

namespace palo {

class ExecEnv;
class PlanFragmentExecutor;
class TExportStatusResult;
class TExportTaskRequest;

// used to report to master
struct ExportTaskResult {
    // files exported to
    std::vector<std::string> files;
};

// used to report to master
struct ExportTaskCtx {
    Status status;
    ExportTaskResult result;
};

// we need to communicate with FE on the status of export tasks, so we need this class to manage.
class ExportTaskMgr {
public:
    ExportTaskMgr(ExecEnv* exec_env);

    virtual ~ExportTaskMgr();

    Status init();

    Status start_task(const TExportTaskRequest& request);

    Status cancel_task(const TUniqueId& id);

    Status erase_task(const TUniqueId& id);

    Status finish_task(const TUniqueId& id, const Status& status, const ExportTaskResult& result);

    Status get_task_state(const TUniqueId& id, TExportStatusResult* status_result);

    void finalize_task(PlanFragmentExecutor* executor);

private:
    void report_to_master(PlanFragmentExecutor* executor);

    ExecEnv* _exec_env;

    std::mutex _lock;
    std::unordered_set<TUniqueId> _running_tasks;
    LruCache<TUniqueId, ExportTaskCtx> _success_tasks;
    LruCache<TUniqueId, ExportTaskCtx> _failed_tasks;
};

} // end namespace palo

#endif // BDG_PALO_BE_SRC_RUNTIME_EXPORT_TASK_MGR_H

