/*
 * Copyright 2024 allurx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package red.zyc.babydogepaws.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import red.zyc.babydogepaws.dao.UserMapper;
import red.zyc.babydogepaws.game.BabyDogePawsTask;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.model.response.base.Response;
import red.zyc.babydogepaws.model.response.base.ResponseMessage;

import java.util.Optional;

import static red.zyc.babydogepaws.common.constant.Constants.VOID;
import static red.zyc.babydogepaws.model.response.base.Response.ok;
import static red.zyc.babydogepaws.model.response.base.ResponseMessage.ILLEGAL_MINE_COUNT;

/**
 * @author allurx
 */
@Tag(name = "Task", description = "BabyDogePaws Task")
@RequestMapping("/task")
@RestController
public class TaskController {

    private final UserMapper userMapper;
    private final BabyDogePawsTask babyDogePawsTask;

    public TaskController(UserMapper userMapper, BabyDogePawsTask babyDogePawsTask) {
        this.userMapper = userMapper;
        this.babyDogePawsTask = babyDogePawsTask;
    }

    @Operation(summary = "启动用户所有定时任务")
    @PostMapping("/bootstrap")
    public Response<Void> bootstrap(//@Parameter(ref = PARAMETER_COMPONENT_USER_PHONE_NUMBER)
                                    String phoneNumber) {
        return Optional.ofNullable(userMapper.getBabyDogeUser(phoneNumber))
                .map(user -> {
                    babyDogePawsTask.schedule(new BabyDogePawsGameRequestParam(user));
                    return ok(VOID);
                })
                .orElse(ok(ResponseMessage.MISSING_USER));
    }

    @Operation(summary = "修改挖矿数量")
    @PostMapping("/updateMineCount")
    public Response<Void> updateMineCount(@RequestParam int mineCountMin, @RequestParam int mineCountMax) {
        if (mineCountMin >= mineCountMax) {
            return ok(ILLEGAL_MINE_COUNT);
        } else {
            BabyDogePawsTask.mineCountMin = mineCountMin;
            BabyDogePawsTask.mineCountMax = mineCountMax;
            return ok();
        }
    }
}
