package itba.configserver.controller;

import java.util.List;

final class KnownProperty {
    private static final List<String> BOOLEANS = List.of("true", "false");

    private final String key;
    private final String defaultValue;
    private final List<String> values;
    private final String hint;
    private final boolean refresh;

    private KnownProperty(String key, String defaultValue, List<String> values, String hint, boolean refresh) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.values = values;
        this.hint = hint;
        this.refresh = refresh;
    }

    static List<KnownProperty> fieldOps() {
        return List.of(
                bool("fieldops.features.assignment-suggestions", "true", true),
                bool("fieldops.maintenance.enabled", "false", true),
                text("fieldops.maintenance.message",
                        "FieldOps is under maintenance. Please try again later.",
                        "cualquier texto",
                        true),
                text("fieldops.maintenance.retry-after-seconds", "60", "entero, segundos", true),
                bool("fieldops.notifications.simulate-failure", "false", true),
                bool("fieldops.events.dispatcher.enabled", "true", false),
                text("fieldops.events.dispatch-delay", "500ms", "duración (500ms, 1s, 1m)", false),
                text("fieldops.events.batch-size", "50", "entero ≥ 1", false),
                text("fieldops.events.max-attempts", "5", "entero ≥ 1", false),
                text("fieldops.events.backoff-base", "1s", "duración (1s, 500ms)", false),
                text("fieldops.events.backoff-max", "1m", "duración (1m, 30s)", false));
    }

    private static KnownProperty bool(String key, String defaultValue, boolean refresh) {
        return new KnownProperty(key, defaultValue, BOOLEANS, "true o false", refresh);
    }

    private static KnownProperty text(String key, String defaultValue, String hint, boolean refresh) {
        return new KnownProperty(key, defaultValue, List.of(), hint, refresh);
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public List<String> getValues() {
        return values;
    }

    public String getHint() {
        return hint;
    }

    public boolean isRefresh() {
        return refresh;
    }
}
