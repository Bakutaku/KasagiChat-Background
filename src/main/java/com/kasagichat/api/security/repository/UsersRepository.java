package com.kasagichat.api.security.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kasagichat.api.security.model.Users;

import jakarta.persistence.LockModeType;

/**
 * 本登録済みユーザーの情報を永続化するRepository。
 */
public interface UsersRepository extends JpaRepository<Users,Long>{
    
    /**
     * 公開ユーザーIDからユーザーを取得する。
     *
     * @param publicId 公開ユーザーID
     * @return 該当するユーザー。存在しない場合は空
     */
    Optional<Users> findByPublicId(UUID publicId);

    /**
     * ユーザーを排他ロックして取得する。
     *
     * <p>同じユーザーの報酬受取処理を直列化するために使う。</p>
     *
     * @param id ユーザーの内部ID
     * @return 該当するユーザー。存在しない場合は空
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Users u where u.id = :id")
    Optional<Users> findByIdForUpdate(@Param("id") Long id);
}
