package dev.meyba.justWorld.world;

import org.bukkit.World;

public record WorldCreationResult(World world, long creationTimeMs) {

    public boolean isSuccess() {
        return world != null;
    }

    public String getFormattedTime() {
        if (creationTimeMs < 1000) {
            return creationTimeMs + "ms";
        } else {
            return String.format("%.2fs", creationTimeMs / 1000.0);
        }
    }
}