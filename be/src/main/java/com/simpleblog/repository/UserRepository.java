package com.simpleblog.repository;

import java.util.Optional;

import com.simpleblog.entity.BlogUser;
import org.springframework.data.repository.Repository;

/** Only saving and looking up by email: a user is never changed or deleted through the API. */
public interface UserRepository extends Repository<BlogUser, Long> {

	BlogUser save(BlogUser user);

	Optional<BlogUser> findByEmail(String email);

}
