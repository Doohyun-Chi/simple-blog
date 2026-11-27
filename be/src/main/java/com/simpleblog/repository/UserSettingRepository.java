package com.simpleblog.repository;

import java.util.Optional;

import com.simpleblog.entity.UserSetting;
import org.springframework.data.repository.Repository;

/** Saving and looking up the one row of a user; a row is never deleted through the API. */
public interface UserSettingRepository extends Repository<UserSetting, Long> {

	UserSetting save(UserSetting setting);

	Optional<UserSetting> findById(Long userId);

}
