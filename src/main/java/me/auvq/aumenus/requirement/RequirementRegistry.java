package me.auvq.aumenus.requirement;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.action.Action;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RequirementRegistry {

    private final Map<String, RequirementEvaluator> evaluators = new HashMap<>();

    public RequirementRegistry() {
        for (RequirementType type : RequirementType.values()) {
            for (String alias : type.getAliases()) {
                evaluators.put(alias.toLowerCase(), type.getEvaluator());
            }
        }
    }

    public void register(@NotNull String key, @NotNull RequirementEvaluator evaluator) {
        evaluators.put(key.toLowerCase(), evaluator);
    }

    public boolean evaluate(@NotNull Player player, @NotNull Requirement requirement) {
        String type = requirement.getType().toLowerCase();
        boolean inverted = false;

        if (type.startsWith("!")) {
            inverted = true;
            type = type.substring(1);
        }

        RequirementEvaluator evaluator = evaluators.get(type);
        if (evaluator == null) {
            AuMenus.getInstance().getLogger().warning("Unknown requirement type: " + requirement.getType());
            return false;
        }

        boolean result = evaluator.evaluate(player, requirement.getConfig());
        return inverted != result;
    }

    public @NotNull RequirementList parseRequirementConfig(@Nullable Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return emptyList();
        }

        if (map.containsKey("checks")) {
            return parseFullForm(map);
        }

        if (map.containsKey("type")) {
            return parseSingleRequirement(map);
        }

        return parseShorthand(map);
    }

    private @NotNull RequirementList parseShorthand(@NotNull Map<String, Object> map) {
        List<Requirement> requirements = new ArrayList<>();

        if (map.containsKey("perm")) {
            requirements.add(buildSimpleRequirement("perm", "has_permission",
                    Map.of("permission", map.get("perm"))));
        }

        if (map.containsKey("money")) {
            requirements.add(buildSimpleRequirement("money", "has_money",
                    Map.of("amount", map.get("money"))));
        }

        if (map.containsKey("exp")) {
            Map<String, Object> expConfig = new HashMap<>();
            expConfig.put("amount", map.get("exp"));
            if (map.containsKey("level")) {
                expConfig.put("level", map.get("level"));
            }
            requirements.add(buildSimpleRequirement("exp", "has_exp", expConfig));
        }

        if (map.containsKey("item")) {
            String itemStr = map.get("item").toString();
            String[] parts = itemStr.split("\\s+");
            Map<String, Object> itemConfig = new HashMap<>();
            itemConfig.put("material", parts[0]);
            if (parts.length > 1) {
                itemConfig.put("amount", Integer.parseInt(parts[1]));
            }
            requirements.add(buildSimpleRequirement("item", "has_item", itemConfig));
        }

        List<Action> denyActions = map.containsKey("deny")
                ? parseActionList(map.get("deny")) : List.of();

        return RequirementList.builder()
                .requirements(requirements)
                .minimumRequired(requirements.size())
                .stopAtSuccess(false)
                .denyActions(denyActions)
                .successActions(List.of())
                .build();
    }

    private @NotNull Requirement buildSimpleRequirement(@NotNull String name, @NotNull String type,
                                                          @NotNull Map<String, Object> config) {
        return Requirement.builder()
                .name(name)
                .type(type)
                .config(config)
                .denyActions(List.of())
                .successActions(List.of())
                .build();
    }

    @SuppressWarnings("unchecked")
    private @NotNull RequirementList parseFullForm(@NotNull Map<String, Object> map) {
        Map<String, Object> checks = (Map<String, Object>) map.get("checks");
        List<Requirement> requirements = new ArrayList<>();

        for (Map.Entry<String, Object> entry : checks.entrySet()) {
            Map<String, Object> checkConfig = (Map<String, Object>) entry.getValue();
            String type = (String) checkConfig.get("type");
            if (type == null) {
                continue;
            }

            List<Action> checkDeny = checkConfig.containsKey("deny")
                    ? parseActionList(checkConfig.get("deny")) : List.of();
            List<Action> checkSuccess = checkConfig.containsKey("success")
                    ? parseActionList(checkConfig.get("success")) : List.of();
            boolean optional = Boolean.TRUE.equals(checkConfig.get("optional"));

            Map<String, Object> evalConfig = new LinkedHashMap<>(checkConfig);
            evalConfig.remove("type");
            evalConfig.remove("deny");
            evalConfig.remove("success");
            evalConfig.remove("optional");

            requirements.add(Requirement.builder()
                    .name(entry.getKey())
                    .type(type)
                    .optional(optional)
                    .config(evalConfig)
                    .denyActions(checkDeny)
                    .successActions(checkSuccess)
                    .build());
        }

        int minimum = map.containsKey("minimum") ? ((Number) map.get("minimum")).intValue() : 0;
        boolean stopAtSuccess = Boolean.TRUE.equals(map.get("stop_at_success"));
        List<Action> denyActions = map.containsKey("deny") ? parseActionList(map.get("deny")) : List.of();

        return RequirementList.builder()
                .requirements(requirements)
                .minimumRequired(minimum)
                .stopAtSuccess(stopAtSuccess)
                .denyActions(denyActions)
                .successActions(List.of())
                .build();
    }

    private @NotNull RequirementList parseSingleRequirement(@NotNull Map<String, Object> map) {
        String type = (String) map.get("type");
        Map<String, Object> config = new LinkedHashMap<>(map);
        config.remove("type");
        config.remove("deny");

        List<Action> denyActions = map.containsKey("deny") ? parseActionList(map.get("deny")) : List.of();

        Requirement req = Requirement.builder()
                .name("single")
                .type(type)
                .config(config)
                .denyActions(denyActions)
                .successActions(List.of())
                .build();

        return RequirementList.builder()
                .requirements(List.of(req))
                .minimumRequired(1)
                .stopAtSuccess(false)
                .denyActions(denyActions)
                .successActions(List.of())
                .build();
    }

    private @NotNull List<Action> parseActionList(@Nullable Object raw) {
        if (raw instanceof String str) {
            return List.of(new Action("msg", str));
        }
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }

        List<Action> actions = new ArrayList<>();
        for (Object item : list) {
            Action parsed = parseSingleAction(item);
            if (parsed != null) {
                actions.add(parsed);
            }
        }
        return actions;
    }

    private static final Set<String> BARE_ACTION_TYPES = Set.of(
            "close", "refresh", "prev_page", "next_page"
    );

    private @Nullable Action parseSingleAction(@NotNull Object item) {
        if (item instanceof String str) {
            if (BARE_ACTION_TYPES.contains(str.toLowerCase())) {
                return new Action(str.toLowerCase(), "");
            }
            String[] parts = str.split(":\\s*", 2);
            if (parts.length == 2) {
                return new Action(parts[0].trim(), parts[1].trim());
            }
            return new Action("msg", str);
        }
        if (!(item instanceof Map<?, ?> map)) {
            return null;
        }
        Map.Entry<?, ?> first = map.entrySet().iterator().next();
        String value = first.getValue() != null ? first.getValue().toString() : "";
        return new Action(first.getKey().toString(), value);
    }

    private static @NotNull RequirementList emptyList() {
        return RequirementList.builder()
                .requirements(List.of())
                .minimumRequired(0)
                .stopAtSuccess(false)
                .denyActions(List.of())
                .successActions(List.of())
                .build();
    }
}
