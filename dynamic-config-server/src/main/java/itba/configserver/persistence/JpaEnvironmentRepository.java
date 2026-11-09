package itba.configserver.persistence;

import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Profile("jpa-backend")
public class JpaEnvironmentRepository implements EnvironmentRepository, Ordered {
    private final PropertyRepository propertyRepository;

    public JpaEnvironmentRepository(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    /**
     * One property source per profile, highest priority first. Later profiles in the request
     * win over earlier ones, and {@code default} is always the lowest-priority base layer.
     * Sources stay even when empty so a refresh can clear values the client already had.
     */
    @Override
    public Environment findOne(String application, String profile, String label) {
        String[] profiles = profile.split(",");
        Environment environment = new Environment(application, profiles, label, null, null);

        List<String> byPriority = new ArrayList<>(List.of(profiles));
        Collections.reverse(byPriority);
        byPriority.add("default");

        byPriority.stream().map(String::trim).distinct().forEach(prof -> {
            Map<String, String> source = propertyRepository.findByApplicationAndProfile(application, prof)
                    .stream()
                    .collect(Collectors.toMap(Property::getKey, Property::getValue, (_, v2) -> v2,
                            LinkedHashMap::new));
            environment.add(new PropertySource("db:" + application + "-" + prof, source));
        });
        return environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
