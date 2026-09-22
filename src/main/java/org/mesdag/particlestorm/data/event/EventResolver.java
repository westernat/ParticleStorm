package org.mesdag.particlestorm.data.event;

import org.mesdag.particlestorm.PSDiagnostics;
import org.mesdag.particlestorm.api.IEventNode;

import java.util.Map;

/// Unified runtime event-id resolution for all event trigger paths.
///
/// Empty (or null) ids are treated as no-op without diagnostics; unknown ids are
/// reported once via PSDiagnostics and skipped, so a dangling reference can
/// never fail the whole resource reload or cause an NPE while ticking.
public final class EventResolver {
    private EventResolver() {
    }

    public static Map<String, IEventNode> resolve(Map<String, Map<String, IEventNode>> events, String id) {
        if (id == null || id.isEmpty()) return Map.of();
        Map<String, IEventNode> nodes = events == null ? null : events.get(id);
        if (nodes == null) {
            PSDiagnostics.warnOnce("unknown-event-id:" + id, "Unknown event id: {}, trigger skipped", id);
            return Map.of();
        }
        return nodes;
    }
}
