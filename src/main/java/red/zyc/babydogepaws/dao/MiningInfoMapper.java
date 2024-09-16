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
package red.zyc.babydogepaws.dao;

import org.apache.ibatis.annotations.Insert;

/**
 * @author allurx
 */
public interface MiningInfoMapper {

    @Insert("""
            INSERT INTO mining_info (user_id, earn_per_tap, count, mined,remaining_energy,draw ) 
            VALUES (#{userId},#{earnPerTap},#{count},#{mined},#{remainingEnergy},#{draw})
                        """)
    int saveMiningInfo(Integer userId, int earnPerTap, int count, int mined, int remainingEnergy, String draw);
}
