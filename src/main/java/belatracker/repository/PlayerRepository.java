package belatracker.repository;

import belatracker.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByNameContainingIgnoreCase(String name);
    List<Player> findAllByOrderByWinsDescLossesAsc();
}
