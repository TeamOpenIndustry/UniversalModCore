package cam72cam.mod.entity;

import cam72cam.mod.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeatEntityTest {
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int FORWARD = 4;
    private static final int BACK = 8;
    private static final int SPRINT = 16;
    private static final double DELTA = 1.0e-9;

    @Test
    void inputExpiresAfterTenTicksAndBelongsToItsSender() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        int input = FORWARD | SPRINT;

        assertEquals(input, SeatEntity.PassengerInputNetwork.samplePassengerInput(owner, owner, 100, 100, input));
        assertEquals(input, SeatEntity.PassengerInputNetwork.samplePassengerInput(owner, owner, 110, 100, input));
        assertEquals(0, SeatEntity.PassengerInputNetwork.samplePassengerInput(owner, owner, 111, 100, input));
        assertEquals(0, SeatEntity.PassengerInputNetwork.samplePassengerInput(owner, owner, 99, 100, input));
        assertEquals(0, SeatEntity.PassengerInputNetwork.samplePassengerInput(other, owner, 100, 100, input));
        assertEquals(0, SeatEntity.PassengerInputNetwork.samplePassengerInput(owner, null, 100, 100, input));
    }

    @Test
    void movementUsesLocalAxesAndSprintSpeed() {
        assertMovement(LEFT, 0.2, 0);
        assertMovement(RIGHT, -0.2, 0);
        assertMovement(FORWARD, 0, 0.2);
        assertMovement(BACK, 0, -0.2);
        assertMovement(LEFT | SPRINT, 0.4, 0);
        assertMovement(BACK | SPRINT, 0, -0.4);
    }

    @Test
    void oppositeInputsCancelAndSprintAloneDoesNotMove() {
        assertMovement(0, 0, 0);
        assertMovement(SPRINT, 0, 0);
        assertMovement(LEFT | RIGHT, 0, 0);
        assertMovement(FORWARD | BACK | SPRINT, 0, 0);
        assertMovement(LEFT | RIGHT | BACK, 0, -0.2);
    }

    @Test
    void diagonalMovementIsNormalizedAtBothSpeeds() {
        double diagonal = 0.2 / Math.sqrt(2);
        assertMovement(LEFT | FORWARD, diagonal, diagonal);
        assertMovement(RIGHT | BACK, -diagonal, -diagonal);
        assertMovement(RIGHT | FORWARD | SPRINT, -2 * diagonal, 2 * diagonal);
    }

    private static void assertMovement(int input, double x, double z) {
        Vec3d actual = SeatEntity.PassengerInputNetwork.passengerMovement(input);
        assertEquals(x, actual.x, DELTA);
        assertEquals(0, actual.y, DELTA);
        assertEquals(z, actual.z, DELTA);
    }
}
