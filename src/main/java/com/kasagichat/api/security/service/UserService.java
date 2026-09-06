package com.kasagichat.api.security.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.kasagichat.api.security.exception.UserNotFoundException;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@RequiredArgsConstructor 
@Slf4j 
public class UserService {
    
    private final UsersRepository usersRepository;

    /**
     * 公開ユーザーIDからユーザーを取得する。
     * @param publicId 公開ユーザーID
     * @return 該当するユーザー
     */
    public Users getCurrentUser(UUID publicId) {
        return usersRepository.findByPublicId(publicId)
            .orElseThrow(() -> {
                log.warn("ユーザーが見つかりませんでした。publicId: {}", publicId);
                return new UserNotFoundException();
            });
    }

        /**
     * 公開ユーザーIDからユーザーを取得する。
     * @param id ユーザーID
     * @return 該当するユーザー
     */
    public Users getCurrentUser(Long id) {
        return usersRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("ユーザーが見つかりませんでした。id: {}", id);
                return new UserNotFoundException();
            });
    }
}
