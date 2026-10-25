package com.alakaiigaming.simplehandcuffs;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChainRenderer {

    private static final double PARTICLE_SPACING = 0.25;
    private static final double LINK_LENGTH = 0.4;
    private static final int MAX_LINKS = 48;
    private static final float LINK_WIDTH = 0.5f;

    private final SimpleHandcuffs plugin;
    private final Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(105, 107, 112), 0.6f);
    private final Map<UUID, List<BlockDisplay>> links = new HashMap<>();
    private BlockData chainBlock;

    ChainRenderer(SimpleHandcuffs plugin) {
        this.plugin = plugin;
    }

    public void draw(Player officer, Player prisoner) {
        if (!plugin.getConfig().getBoolean("chain.enabled", true)) {
            remove(prisoner.getUniqueId());
            return;
        }
        if (!officer.getWorld().equals(prisoner.getWorld())) return;

        List<Location> points = curve(handOf(officer), prisoner.getLocation().add(0, 0.9, 0));

        if ("links".equalsIgnoreCase(plugin.getConfig().getString("chain.style", "particles")) && chainBlock() != null) {
            drawLinks(prisoner.getUniqueId(), points);
        } else {
            remove(prisoner.getUniqueId());
            World world = points.get(0).getWorld();
            for (Location point : points) {
                world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
            }
        }
    }

    private Location handOf(Player player) {
        Location base = player.getLocation();
        double yaw = Math.toRadians(base.getYaw());
        Vector forward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector right = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        return base.add(0, 0.9, 0).add(right.multiply(0.35)).add(forward.multiply(0.3));
    }

    private List<Location> curve(Location a, Location b) {
        double distance = a.distance(b);
        double sag = Math.min(1.0, distance * 0.08);
        int steps = Math.max(2, (int) Math.ceil(distance / PARTICLE_SPACING));
        List<Location> points = new ArrayList<>(steps + 1);
        Vector delta = b.toVector().subtract(a.toVector());
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Location point = a.clone().add(delta.clone().multiply(t));
            point.subtract(0, sag * 4 * t * (1 - t), 0);
            points.add(point);
        }
        return points;
    }

    private void drawLinks(UUID prisoner, List<Location> curve) {
        List<Location> joints = new ArrayList<>();
        joints.add(curve.get(0));
        double carried = 0;
        for (int i = 1; i < curve.size() && joints.size() <= MAX_LINKS; i++) {
            carried += curve.get(i).distance(curve.get(i - 1));
            if (carried >= LINK_LENGTH || i == curve.size() - 1) {
                joints.add(curve.get(i));
                carried = 0;
            }
        }

        int needed = joints.size() - 1;
        List<BlockDisplay> list = links.computeIfAbsent(prisoner, k -> new ArrayList<>());
        World world = joints.get(0).getWorld();

        list.removeIf(display -> {
            boolean gone = !display.isValid() || !display.getWorld().equals(world);
            if (gone) display.remove();
            return gone;
        });
        while (list.size() > needed) list.remove(list.size() - 1).remove();
        while (list.size() < needed) {
            BlockDisplay display = world.spawn(joints.get(0), BlockDisplay.class, d -> {
                d.setBlock(chainBlock);
                d.setPersistent(false);
                d.setTeleportDuration(2);
                d.setInterpolationDuration(2);
            });
            list.add(display);
        }

        for (int i = 0; i < needed; i++) {
            Location start = joints.get(i);
            Location end = joints.get(i + 1);
            Vector dir = end.toVector().subtract(start.toVector());
            float length = (float) dir.length();
            if (length < 0.001f) continue;

            Location center = start.clone().add(dir.clone().multiply(0.5));
            center.setYaw(0);
            center.setPitch(0);

            Vector3f axis = new Vector3f((float) dir.getX(), (float) dir.getY(), (float) dir.getZ()).normalize();
            Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), axis);
            Vector3f scale = new Vector3f(LINK_WIDTH, length + 0.05f, LINK_WIDTH);
            Vector3f offset = rotation.transform(new Vector3f(-0.5f * scale.x, -0.5f * scale.y, -0.5f * scale.z));

            BlockDisplay display = list.get(i);
            display.teleport(center);
            display.setInterpolationDelay(0);
            display.setTransformation(new Transformation(offset, rotation, scale, new Quaternionf()));
        }
    }

    // CHAIN became IRON_CHAIN in newer versions
    private BlockData chainBlock() {
        if (chainBlock == null) {
            Material material = Material.matchMaterial("IRON_CHAIN");
            if (material == null) material = Material.matchMaterial("CHAIN");
            if (material != null && material.isBlock()) chainBlock = material.createBlockData();
        }
        return chainBlock;
    }

    public void remove(UUID prisoner) {
        List<BlockDisplay> list = links.remove(prisoner);
        if (list != null) list.forEach(BlockDisplay::remove);
    }

    public void clearAll() {
        links.values().forEach(list -> list.forEach(BlockDisplay::remove));
        links.clear();
    }
}
