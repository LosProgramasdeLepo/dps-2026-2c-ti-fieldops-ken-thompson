package itba.configserver.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findByApplicationAndProfile(String application, String profile);

    List<Property> findByApplication(String application);

    Optional<Property> findByApplicationAndProfileAndLabelAndKey(String application, String profile, String label,
            String key);
}
