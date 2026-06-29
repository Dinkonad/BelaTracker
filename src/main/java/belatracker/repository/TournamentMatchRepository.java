package belatracker.repository;

import belatracker.model.TournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {
    List<TournamentMatch> findByTournamentIdOrderByRoundNoAscSlotAsc(Long tournamentId);
    List<TournamentMatch> findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(Long tournamentId, String phase);
    void deleteByTournamentId(Long tournamentId);
}
