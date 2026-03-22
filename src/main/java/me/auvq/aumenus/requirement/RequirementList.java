package me.auvq.aumenus.requirement;

import lombok.Builder;
import lombok.Getter;
import me.auvq.aumenus.action.Action;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public final class RequirementList {

    private final @NotNull List<Requirement> requirements;
    private final int minimumRequired;
    private final boolean stopAtSuccess;
    private final @NotNull List<Action> denyActions;
    private final @NotNull List<Action> successActions;

    public boolean evaluate(@NotNull Player player, @NotNull RequirementRegistry registry) {
        if (requirements.isEmpty()) {
            return true;
        }

        int passed = 0;

        for (Requirement req : requirements) {
            boolean result = registry.evaluate(player, req);

            if (result) {
                passed++;
                if (stopAtSuccess && minimumRequired > 0 && passed >= minimumRequired) {
                    return true;
                }
                continue;
            }

            if (req.isOptional()) {
                continue;
            }
        }

        int target = minimumRequired > 0 ? minimumRequired : (requirements.size() - countOptional());
        return passed >= target;
    }

    public @NotNull EvaluationResult evaluateDetailed(@NotNull Player player,
                                                       @NotNull RequirementRegistry registry) {
        if (requirements.isEmpty()) {
            return new EvaluationResult(true, List.of());
        }

        List<Requirement> failed = new ArrayList<>();
        int passed = 0;
        int target = minimumRequired > 0 ? minimumRequired : (requirements.size() - countOptional());

        for (Requirement req : requirements) {
            if (registry.evaluate(player, req)) {
                passed++;
                if (stopAtSuccess && passed >= target) {
                    return new EvaluationResult(true, List.of());
                }
                continue;
            }

            if (req.isOptional()) {
                continue;
            }

            failed.add(req);
        }

        return new EvaluationResult(passed >= target, failed);
    }

    private int countOptional() {
        int count = 0;
        for (Requirement req : requirements) {
            if (req.isOptional()) {
                count++;
            }
        }
        return count;
    }

    public @NotNull List<Requirement> getFailedRequirements(@NotNull Player player,
                                                             @NotNull RequirementRegistry registry) {
        return requirements.stream()
                .filter(req -> !registry.evaluate(player, req))
                .toList();
    }

    public record EvaluationResult(boolean passed, @NotNull List<Requirement> failed) {}
}
