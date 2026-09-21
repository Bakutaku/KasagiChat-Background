package com.kasagichat.api.conversation.controller.dto.response;

import com.kasagichat.api.npc.model.Topic;

/**
 * 振り返りで新たに覚えた話題。
 */
public final class NewTopicResponse {

    private final Long id;
    private final String name;
    private final boolean publicTopic;

    public NewTopicResponse(Long id, String name, boolean publicTopic) {
        this.id = id;
        this.name = name;
        this.publicTopic = publicTopic;
    }

    public static NewTopicResponse from(Topic topic) {
        return new NewTopicResponse(topic.getId(), topic.getName(), topic.getPublicTopic());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * JavaBeansの規約によりJSONプロパティ名は {@code public} になる。
     *
     * @return イベント等へ公開してよいか
     */
    public boolean isPublic() {
        return publicTopic;
    }
}
