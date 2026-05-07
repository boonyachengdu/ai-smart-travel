package com.boonya.business.trip.dialog.agent.scene;

import com.boonya.business.trip.common.constant.Scene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SceneHandlerFactory {

    private final List<SceneHandler> handlers;
    private final Map<Scene, SceneHandler> handlerMap = new ConcurrentHashMap<>();

    public SceneHandler getHandler(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException("场景不能为空");
        }

        return handlerMap.computeIfAbsent(scene, s -> {
            SceneHandler handler = handlers.stream()
                    .filter(h -> h.supports(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("不支持的场景：" + s));
            log.info("注册场景处理器：{} -> {}", s, handler.getClass().getSimpleName());
            return handler;
        });
    }

    public boolean isSupported(Scene scene) {
        return handlers.stream().anyMatch(h -> h.supports(scene));
    }
}
