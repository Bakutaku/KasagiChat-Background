package com.kasagichat.api.security.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.security.model.Users;

/**
 * ユーザー情報のリポジトリ
 */
public interface UsersRepository extends JpaRepository<Users,Long>{
    
    /**
     * 公開ユーザーIDからユーザーを取得する。
     *
     * @param publicId 公開ユーザーID
     * @return 該当するユーザー。存在しない場合は空
     */
    Optional<Users> findByPublicId(UUID publicId);
}
