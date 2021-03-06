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

#ifndef BDG_PALO_BE_RUNTIME_FRAGMENT_MGR_H
#define BDG_PALO_BE_RUNTIME_FRAGMENT_MGR_H

#include <mutex>
#include <memory>
#include <unordered_map>
#include <functional>
#include <thread>

#include "common/status.h"
#include "gen_cpp/Types_types.h"
#include "gen_cpp/internal_service.pb.h"
#include "util/thread_pool.hpp"
#include "util/hash_util.hpp"
#include "http/rest_monitor_iface.h"

namespace palo {

class ExecEnv;
class FragmentExecState;
class TExecPlanFragmentParams;
class PlanFragmentExecutor;

// This class used to manage all the fragment execute in this instance
class FragmentMgr : public RestMonitorIface {
public:
    typedef std::function<void (PlanFragmentExecutor*)> FinishCallback;

    FragmentMgr(ExecEnv* exec_env);
    virtual ~FragmentMgr();

    // execute one plan fragment
    Status exec_plan_fragment(const TExecPlanFragmentParams& params);

    // TODO(zc): report this is over
    Status exec_plan_fragment(const TExecPlanFragmentParams& params, FinishCallback cb);

    Status fetch_fragment_exec_infos(PFetchFragmentExecInfosResult* result,
                                     const PFetchFragmentExecInfoRequest* request);

    Status cancel(const TUniqueId& fragment_id);

    void cancel_worker();

    virtual void debug(std::stringstream& ss);
private:
    void exec_actual(std::shared_ptr<FragmentExecState> exec_state,
                     FinishCallback cb);

    // This is input params
    ExecEnv* _exec_env;

    std::mutex _lock;

    // Make sure that remove this before no data reference FragmentExecState
    std::unordered_map<TUniqueId, std::shared_ptr<FragmentExecState>> _fragment_map;

    // Cancel thread
    bool _stop;
    std::thread _cancel_thread;
    // every job is a pool
    ThreadPool _thread_pool;

};

}

#endif

