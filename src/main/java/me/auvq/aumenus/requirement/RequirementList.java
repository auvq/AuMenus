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
            return new EvaluationResult(true, List.of(), List.of(), List.of());
        }

        List<Requirement> failed = new ArrayList<>();
        List<Requirement> failedOptional = new ArrayList<>();
        List<Requirement> passedList = new ArrayList<>();
        int passedCount = 0;
        int target = minimumRequired > 0 ? minimumRequired : (requirements.size() - countOptional());

        for (Requirement req : requirements) {
            if (registry.evaluate(player, req)) {
                passedCount++;
                passedList.add(req);
                if (stopAtSuccess && passedCount >= target) {
                    return new EvaluationResult(true, List.of(), passedList, failedOptional);
                }
                continue;
            }

            if (req.isOptional()) {
                failedOptional.add(req);
                continue;
            }

            failed.add(req);
        }

        return new EvaluationResult(passedCount >= target, failed, passedList, failedOptional);
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

    public record EvaluationResult(boolean passed, @NotNull List<Requirement> failed,
                                       @NotNull List<Requirement> passed_list,
                                       @NotNull List<Requirement> failedOptional) {}
}
