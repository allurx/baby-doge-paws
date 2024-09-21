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

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import red.zyc.babydogepaws.model.persistent.UserConfig;

/**
 * @author allurx
 */
public interface UserConfigMapper {

    @Select("""
            select * from user_config where user_id=#{userId}
            """)
    UserConfig getUserConfig(Integer userId);

    @Update("""
            update user_config set user_agent = #{userAgent} where user_id=#{userId}
            """)
    int updateUserAgent(Integer userId, String userAgent);
}
