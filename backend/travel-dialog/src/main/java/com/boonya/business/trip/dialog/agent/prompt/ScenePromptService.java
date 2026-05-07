package com.boonya.business.trip.dialog.agent.prompt;

import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.dialog.agent.scene.SceneHandler;
import com.boonya.business.trip.dialog.agent.scene.SceneHandlerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
/**
 * @ClassName: ScenePromptService
 * @Description:  智能商旅场景提示词
 */
@Service
@RequiredArgsConstructor
public class ScenePromptService {

    private final SceneHandlerFactory sceneHandlerFactory;

    /**
     * 构建政策场景提示词
     *
     * @param scene
     * @param ragPolicyText
     * @return
     */
    public String buildPrompt(Scene scene, String ragPolicyText) {
        String basePrompt;

        if (scene != null && sceneHandlerFactory.isSupported(scene)) {
            SceneHandler handler = sceneHandlerFactory.getHandler(scene);
            basePrompt = handler.getOrderPrompt();
        } else {
            basePrompt = SystemPrompt.SYSTEM_PROMPT;
        }

        StringBuilder promptBuilder = new StringBuilder(basePrompt);
        promptBuilder.append("\n现在时间是：").append(LocalDate.now());

        if (ragPolicyText != null && !ragPolicyText.isEmpty()) {
            promptBuilder.append("\n\n【企业差旅政策】\n").append(ragPolicyText);
        }

        return promptBuilder.toString();
    }
}
