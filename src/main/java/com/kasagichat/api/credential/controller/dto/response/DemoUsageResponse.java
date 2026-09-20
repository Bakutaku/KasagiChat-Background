package com.kasagichat.api.credential.controller.dto.response;

/**
 * アカウント単位のデモ利用回数。
 *
 * @param used 使用済み回数
 * @param limit 上限回数
 * @param remaining 残り回数
 */
public record DemoUsageResponse(int used, int limit, int remaining) {
}
