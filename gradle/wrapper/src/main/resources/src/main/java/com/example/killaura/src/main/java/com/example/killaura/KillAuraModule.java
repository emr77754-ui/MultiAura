package com.example.killaura;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class KillAuraModule {
    public boolean enabled = true;
    public double range = 3.4;
    public double wallRange = 3.2;
    public boolean throughWalls = false;
    public boolean shieldBreaker = true;
    public boolean rotateToTarget = true;
    public float rotationSpeed = 180f;
    public boolean backstab = true;
    public boolean dodgeBehind = true;
    public boolean spinOnTarget = true;
    public int spinSpeed = 20;
    public boolean prioritizeLowHP = true;
    public boolean prioritizeShield = true;
    public int hitDelay = 1;
    public boolean sprintReset = true;
    public boolean autoJumpReset = true;

    private LivingEntity target;
    private int cooldown = 0;
    private int spinTicks = 0;
    private boolean wasSprinting = false;

    public void tick(MinecraftClient mc) {
        if (!enabled || mc.player == null || mc.world == null) {
            target = null;
            return;
        }

        if (target == null || !isValid(target, mc)) {
            target = findBestTarget(mc);
        }

        if (target == null) return;

        double dist = mc.player.distanceTo(target);
        double maxRange = throughWalls ? wallRange : range;
        if (dist > maxRange) return;

        if (shieldBreaker && isBlocking(target) && backstab) {
            moveBehind(mc, target);
        }

        if (spinOnTarget) {
            spinTicks++;
            if (spinTicks % spinSpeed == 0) {
                mc.player.setYaw((float) ((mc.player.getYaw() + 35) % 360));
            }
        }

        if (rotateToTarget) rotateTo(mc, target);

        if (autoJumpReset && mc.player.isOnGround() && mc.player.fallDistance <= 0) {
            mc.player.jump();
        }

        if (sprintReset && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
            wasSprinting = true;
        }

        if (cooldown <= 0) {
            doAttack(mc, target);
            cooldown = hitDelay;
        } else cooldown--;

        if (dodgeBehind && wasSprinting) {
            moveBehind(mc, target);
            wasSprinting = false;
        }
    }

    private LivingEntity findBestTarget(MinecraftClient mc) {
        List<LivingEntity> candidates = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive() && !p.isSpectator())
                .filter(p -> mc.player.distanceTo(p) <= range + 0.3)
                .filter(p -> throughWalls || mc.player.canSee(p))
                .sorted(Comparator.comparingDouble(p -> score(mc, p)))
                .collect(Collectors.toList());
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private double score(MinecraftClient mc, LivingEntity e) {
        double s = mc.player.distanceTo(e);
        if (prioritizeLowHP) s += e.getHealth() * 0.8;
        if (prioritizeShield && isBlocking(e)) s -= 2.0;
        if (e.getHealth() < 8f) s -= 1.0;
        return s;
    }

    private boolean isValid(LivingEntity e, MinecraftClient mc) {
        return e.isAlive() && e != mc.player && mc.player.distanceTo(e) <= range + 0.5;
    }

    private boolean isBlocking(LivingEntity e) {
        return e.isBlocking() || (e instanceof PlayerEntity p && p.isBlocking());
    }

    private void rotateTo(MinecraftClient mc, LivingEntity target) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d tp = target.getPos().add(0, target.getHeight() * 0.85, 0);
        Vec3d diff = tp.subtract(eye);
        float yaw = (float) (Math.toDegrees(Math.atan2(-diff.x, diff.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z))));
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private void moveBehind(MinecraftClient mc, LivingEntity target) {
        Vec3d targetForward = Vec3d.fromPolar(0, target.getYaw()).normalize();
        Vec3d behindPos = target.getPos().subtract(targetForward.multiply(1.2));
        Vec3d dir = behindPos.subtract(mc.player.getPos());
        double len = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        if (len < 0.1) return;
        double nx = dir.x / len;
        double nz = dir.z / len;
        mc.player.setYaw((float) Math.toDegrees(Math.atan2(-nx, nz)));
        mc.player.setVelocity(nx * 0.28, mc.player.getVelocity().y, nz * 0.28);
    }

    private void doAttack(MinecraftClient mc, LivingEntity target) {
        if (mc.interactionManager == null) return;
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
