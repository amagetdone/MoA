package com.ssafy.amagetdone.domain.account.model.repository;

import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.model.entity.Account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, String> {

	@Query("select a from Account a where a.user.id = :userId and a.accountNo = :accountNo")
	Optional<Account> findByUserIdAndAccountNo(@Param("userId") Integer userId, @Param("accountNo") String accountNo);

	@Query("select a from Account a where a.user.id = :userId order by a.accountNo asc")
	List<Account> findAllByUserId(@Param("userId") Integer userId);

	List<Account> findAllByUser_Id(Integer userId);

	@Query("select case when count(a) > 0 then true else false end from Account a where a.user.id = :userId")
	boolean existsByUserId(@Param("userId") Integer userId);

	@Query("delete from Account a where a.user.id = :userId")
	@org.springframework.data.jpa.repository.Modifying
	void deleteByUserId(@Param("userId") Integer userId);

	boolean existsByUser_Id(Integer userId);

	void deleteByUser_Id(Integer userId);
}
