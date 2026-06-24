package belatracker.repository;

import belatracker.model.player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerRepository extends JpaRepository<player, Long> {
    List<player> findByNameContainingIgnoreCase(String name);
}
