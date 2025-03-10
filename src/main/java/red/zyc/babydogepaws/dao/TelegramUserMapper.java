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

import org.apache.ibatis.annotations.Update;

/**
 * @author allurx
 */
public interface TelegramUserMapper {

    @Update("""
            update telegram_user set banned=#{banned} where id=#{id}
            """)
    int updateTelegramUserBanned(Integer id, int banned);

    @Update("""
            update telegram_user set
                         telegram_user_id=#{telegramUserId}, 
                         username=#{username}
                  where id=#{id}
                        """)
    int saveOrUpdateTelegramUser(Integer id, Long telegramUserId, String username);
}
