package tn.airaware.api.repositories;

import jakarta.nosql.mapping.Repository;
import tn.airaware.api.entities.Coordinates;

/**
 * Repository for Coordinates entity — usually used via embedded queries.
 */

public interface CoordinatesRepository extends Repository<Coordinates, String> {
}
