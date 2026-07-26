package org.com.epicawaken_grappling_hook.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.com.epicawaken_grappling_hook.Config;

public final class GrapplingSwingPhysics {
    private static final double SWING_GRAVITY = 0.08D;
    private static final double TAUT_ROPE_RATIO = 0.98D;
    private static final double MIN_VECTOR_LENGTH_SQR = 1.0E-8D;
    private static final double TURNAROUND_SPEED = 0.04D;

    public record SwingResult(Vec3 velocity, double energy, int travelDirection) {
    }

    private GrapplingSwingPhysics() {
    }

    public static double calculateInitialRopeLength(Entity entity, Vec3 anchor, double maximumRopeLength) {
        double distance = attachmentPosition(entity).distanceTo(anchor);
        return Math.max(Config.PHANTOM_SWING_MIN_ROPE_LENGTH, Math.min(maximumRopeLength, distance));
    }

    public static void applyInitialBoost(
            Entity entity,
            Vec3 anchor,
            double speedRopeLength,
            Vec3 swingPlaneNormal,
            int travelDirection) {
        if (Config.phantomSwingInitialBoost <= 0.0D) {
            return;
        }

        Vec3 anchorToPlayer = attachmentPosition(entity).subtract(anchor);
        if (anchorToPlayer.lengthSqr() < MIN_VECTOR_LENGTH_SQR) {
            return;
        }

        Vec3 normal = anchorToPlayer.normalize();
        Vec3 tangent = forwardTangent(swingPlaneNormal, normal, entity.getDeltaMovement());
        if (tangent.lengthSqr() < MIN_VECTOR_LENGTH_SQR) {
            return;
        }

        double speedScale = calculateSpeedScale(speedRopeLength);
        Vec3 boostedVelocity = entity.getDeltaMovement().add(
                tangent.scale(Config.phantomSwingInitialBoost * speedScale * travelDirection));
        entity.setDeltaMovement(clampSpeed(boostedVelocity, speedScale));
        entity.hurtMarked = true;
    }

    public static double calculateInitialEnergy(
            Entity entity,
            Vec3 anchor,
            double ropeLength,
            double speedRopeLength,
            Vec3 swingPlaneNormal) {
        Vec3 anchorToPlayer = attachmentPosition(entity).subtract(anchor);
        double distance = anchorToPlayer.length();
        if (distance < MIN_VECTOR_LENGTH_SQR) {
            return 0.0D;
        }

        Vec3 normal = anchorToPlayer.scale(1.0D / distance);
        Vec3 tangent = forwardTangent(swingPlaneNormal, normal, entity.getDeltaMovement());
        double forwardSpeed = entity.getDeltaMovement().dot(tangent);
        double speedScale = calculateSpeedScale(speedRopeLength);
        return Math.min(energyCap(), mechanicalEnergy(
                attachmentPosition(entity),
                anchor,
                ropeLength,
                forwardSpeed,
                speedScale));
    }

    public static int calculateInitialTravelDirection(Entity entity, Vec3 anchor, Vec3 swingPlaneNormal) {
        Vec3 anchorToPlayer = attachmentPosition(entity).subtract(anchor);
        if (anchorToPlayer.lengthSqr() < MIN_VECTOR_LENGTH_SQR) {
            return 1;
        }

        Vec3 normal = anchorToPlayer.normalize();
        Vec3 tangent = forwardTangent(swingPlaneNormal, normal, entity.getDeltaMovement());
        Vec3 downTangent = projectOntoTangent(new Vec3(0.0D, -1.0D, 0.0D), normal);
        if (tangent.lengthSqr() < MIN_VECTOR_LENGTH_SQR || downTangent.lengthSqr() < MIN_VECTOR_LENGTH_SQR) {
            return 1;
        }
        return tangent.dot(downTangent) < 0.0D ? -1 : 1;
    }

    public static SwingResult tick(
            Entity entity,
            Vec3 anchor,
            double ropeLength,
            double speedRopeLength,
            Vec3 swingPlaneNormal,
            double storedEnergy,
            int previousTravelDirection) {
        Vec3 anchorToPlayer = attachmentPosition(entity).subtract(anchor);
        double distance = anchorToPlayer.length();
        if (distance < MIN_VECTOR_LENGTH_SQR) {
            return new SwingResult(entity.getDeltaMovement(), storedEnergy, previousTravelDirection);
        }

        Vec3 normal = anchorToPlayer.scale(1.0D / distance);
        Vec3 velocity = entity.getDeltaMovement();
        double speedScale = calculateSpeedScale(speedRopeLength);
        double ropeError = Math.max(0.0D, distance - ropeLength);
        double radialSpeed = velocity.dot(normal);
        if (ropeError > 0.0D) {
            double correctionSpeed = Math.min(
                    Config.phantomSwingMaxCorrectionSpeed,
                    ropeError * Config.phantomSwingConstraintStrength);
            double desiredRadialSpeed = -correctionSpeed;
            velocity = velocity.subtract(normal.scale(radialSpeed - desiredRadialSpeed));
        } else if (distance >= ropeLength * TAUT_ROPE_RATIO) {
            velocity = velocity.subtract(normal.scale(radialSpeed));
        }

        Vec3 forwardTangent = forwardTangent(swingPlaneNormal, normal, velocity);
        if (forwardTangent.lengthSqr() < MIN_VECTOR_LENGTH_SQR) {
            return new SwingResult(clampSpeed(velocity, speedScale), storedEnergy, previousTravelDirection);
        }

        velocity = velocity.add(forwardTangent.scale(-SWING_GRAVITY * speedScale * forwardTangent.y));
        Vec3 attachment = attachmentPosition(entity);
        double forwardSpeed = velocity.dot(forwardTangent);
        double angleCosine = Math.max(-1.0D, Math.min(1.0D, -normal.y));
        double maximumAngleCosine = Math.cos(Math.toRadians(Config.phantomSwingMaxAngleDegrees));
        boolean stopAtMaximumAngle = angleCosine < maximumAngleCosine
                && forwardSpeed * forwardTangent.y > 0.0D;
        if (stopAtMaximumAngle) {
            velocity = velocity.subtract(forwardTangent.scale(forwardSpeed));
            forwardSpeed = 0.0D;
        }
        double currentEnergy = mechanicalEnergy(
                attachment,
                anchor,
                ropeLength,
                forwardSpeed,
                speedScale);
        double energy = Math.max(storedEnergy, currentEnergy);
        energy *= Config.phantomSwingEnergyRetention;
        energy = Math.min(energy, energyCap());
        int travelDirection = previousTravelDirection;
        double turnaroundSpeed = TURNAROUND_SPEED * speedScale;
        if (forwardSpeed > turnaroundSpeed) {
            travelDirection = 1;
        } else if (forwardSpeed < -turnaroundSpeed) {
            travelDirection = -1;
        }
        boolean reversed = travelDirection != previousTravelDirection;
        double travelSign = travelDirection;
        double descentFactor = Math.max(0.0D, -forwardTangent.y * travelSign);
        energy += descentFactor * Config.phantomSwingEnergyGainPerTick;
        energy = Math.min(energy, energyCap());
        if (reversed) {
            energy *= Config.phantomSwingPerSwingEnergyMultiplier;
        }

        double normalizedHeightAboveBottom = normalizedHeightAboveBottom(attachment, anchor, ropeLength);
        double targetKineticEnergy = Math.max(
                0.0D,
                energy - SWING_GRAVITY * normalizedHeightAboveBottom);
        double targetSpeed = Math.min(
                Config.phantomSwingMaxSpeed * speedScale,
                Math.sqrt(2.0D * targetKineticEnergy) * speedScale);
        if (stopAtMaximumAngle) {
            targetSpeed = 0.0D;
        }
        double desiredForwardSpeed = targetSpeed * travelSign;
        if (Math.abs(forwardSpeed) < targetSpeed) {
            velocity = velocity.add(forwardTangent.scale(desiredForwardSpeed - forwardSpeed));
        } else if (Math.abs(forwardSpeed) > targetSpeed) {
            double cappedForwardSpeed = Math.copySign(targetSpeed, forwardSpeed);
            velocity = velocity.add(forwardTangent.scale(cappedForwardSpeed - forwardSpeed));
        }

        velocity = clampSpeed(velocity, speedScale);
        entity.setDeltaMovement(velocity);
        entity.fallDistance = 0.0F;
        entity.hurtMarked = true;
        return new SwingResult(velocity, energy, travelDirection);
    }

    private static double mechanicalEnergy(
            Vec3 attachment,
            Vec3 anchor,
            double ropeLength,
            double forwardSpeed,
            double speedScale) {
        double normalizedForwardSpeed = forwardSpeed / speedScale;
        return 0.5D * normalizedForwardSpeed * normalizedForwardSpeed
                + SWING_GRAVITY * normalizedHeightAboveBottom(attachment, anchor, ropeLength);
    }

    private static double normalizedHeightAboveBottom(Vec3 attachment, Vec3 anchor, double ropeLength) {
        double heightAboveBottom = Math.max(0.0D, attachment.y - (anchor.y - ropeLength));
        return heightAboveBottom * Config.PHANTOM_SWING_TARGET_ROPE_LENGTH / Math.max(ropeLength, MIN_VECTOR_LENGTH_SQR);
    }

    private static double energyCap() {
        return SWING_GRAVITY
                * Config.PHANTOM_SWING_TARGET_ROPE_LENGTH
                * 2.0D
                * Config.phantomSwingEnergyCapRatio;
    }

    private static double calculateSpeedScale(double ropeLength) {
        double clampedRopeLength = Math.max(Config.PHANTOM_SWING_MIN_ROPE_LENGTH, ropeLength);
        return clampedRopeLength / Config.PHANTOM_SWING_TARGET_ROPE_LENGTH;
    }

    public static Vec3 attachmentPosition(Entity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.65D, 0.0D);
    }

    private static Vec3 forwardTangent(Vec3 swingPlaneNormal, Vec3 ropeNormal, Vec3 fallbackVelocity) {
        Vec3 tangent = swingPlaneNormal.cross(ropeNormal);
        if (tangent.lengthSqr() < MIN_VECTOR_LENGTH_SQR) {
            tangent = projectOntoTangent(fallbackVelocity, ropeNormal);
        }
        if (tangent.lengthSqr() < MIN_VECTOR_LENGTH_SQR) {
            tangent = projectOntoTangent(new Vec3(0.0D, 1.0D, 0.0D), ropeNormal);
        }
        if (tangent.lengthSqr() < MIN_VECTOR_LENGTH_SQR) {
            tangent = new Vec3(ropeNormal.z, 0.0D, -ropeNormal.x);
        }
        if (tangent.lengthSqr() < MIN_VECTOR_LENGTH_SQR) {
            return Vec3.ZERO;
        }

        return tangent.normalize();
    }

    public static Vec3 calculateSwingPlaneNormal(Entity entity, Vec3 anchor, Vec3 launchForward) {
        Vec3 horizontalForward = horizontal(launchForward);
        if (horizontalForward.lengthSqr() < MIN_VECTOR_LENGTH_SQR) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }

        Vec3 planeNormal = horizontalForward.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 anchorToPlayer = attachmentPosition(entity).subtract(anchor);
        if (anchorToPlayer.lengthSqr() >= MIN_VECTOR_LENGTH_SQR) {
            Vec3 initialTangent = planeNormal.cross(anchorToPlayer.normalize());
            if (initialTangent.dot(horizontalForward) < 0.0D) {
                planeNormal = planeNormal.reverse();
            }
        }
        return planeNormal;
    }

    private static Vec3 projectOntoTangent(Vec3 vector, Vec3 ropeNormal) {
        return vector.subtract(ropeNormal.scale(vector.dot(ropeNormal)));
    }

    private static Vec3 horizontal(Vec3 vector) {
        if (vector == null) {
            return Vec3.ZERO;
        }
        Vec3 horizontal = new Vec3(vector.x, 0.0D, vector.z);
        return horizontal.lengthSqr() < MIN_VECTOR_LENGTH_SQR ? Vec3.ZERO : horizontal.normalize();
    }

    private static Vec3 clampSpeed(Vec3 velocity, double speedScale) {
        double maxSpeed = Config.phantomSwingMaxSpeed * speedScale;
        double speedSqr = velocity.lengthSqr();
        if (speedSqr <= maxSpeed * maxSpeed) {
            return velocity;
        }
        return velocity.normalize().scale(maxSpeed);
    }
}
