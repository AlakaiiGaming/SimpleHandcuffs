package com.alakaiigaming.simplehandcuffs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

final class FollowTask implements Runnable {

    private final SimpleHandcuffs plugin;

    FollowTask(SimpleHandcuffs plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        double pull = plugin.getConfig().getDouble("follow.pull-distance", 3);
        double teleport = plugin.getConfig().getDouble("follow.teleport-distance", 12);

        for (Map.Entry<UUID, UUID> entry : new ArrayList<>(plugin.cuffs().all().entrySet())) {
            Player prisoner = Bukkit.getPlayer(entry.getKey());
            Player officer = Bukkit.getPlayer(entry.getValue());
            if (prisoner == null || officer == null || prisoner.isDead() || officer.isDead()) {
                plugin.chains().remove(entry.getKey());
                continue;
            }

            Location from = prisoner.getLocation();
            Location to = officer.getLocation();

            if (!from.getWorld().equals(to.getWorld()) || from.distance(to) > teleport) {
                prisoner.teleport(to);
            } else {
                double distance = from.distance(to);
                if (distance > pull) {
                    Vector direction = to.toVector().subtract(from.toVector());
                    double climb = direction.getY();
                    direction.setY(0);
                    if (direction.lengthSquared() > 0.0001) direction.normalize();
                    double speed = Math.min(0.6, 0.25 + (distance - pull) * 0.12);
                    direction.multiply(speed);

                    double y = prisoner.getVelocity().getY();
                    if (climb > 0.8 && ((org.bukkit.entity.Entity) prisoner).isOnGround()) y = 0.42;
                    direction.setY(y);
                    prisoner.setVelocity(direction);
                }
            }

            plugin.chains().draw(officer, prisoner);
        }
    }
}
