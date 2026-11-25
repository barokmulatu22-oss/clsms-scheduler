package service;

/**
 * Functional interface allowing entity managers to check whether an entity ID
 * is currently referenced by any active schedule, without tightly coupling
 * to ScheduleManager before it is built.
 */
@FunctionalInterface
public interface ScheduleReferenceChecker {

    /**
     * Checks if the entity with the specified ID is referenced by an active schedule.
     *
     * @param entityId The normalized ID or code of the entity
     * @return true if referenced by an active schedule, false otherwise
     */
    boolean isReferenced(String entityId);
}
