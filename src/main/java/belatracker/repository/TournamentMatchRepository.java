package belatracker.repository;

import belatracker.model.TournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {
    List<TournamentMatch> findByTournamentIdOrderByRoundNoAscSlotAsc(Long tournamentId);
    List<TournamentMatch> findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(Long tournamentId, String phase);
    Optional<TournamentMatch> findByMatchId(Long matchId);
    void deleteByTournamentId(Long tournamentId);
}
