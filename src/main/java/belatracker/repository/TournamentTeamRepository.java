package belatracker.repository;

import belatracker.model.TournamentTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, Long> {
    List<TournamentTeam> findByTournamentIdOrderByIdAsc(Long tournamentId);
    void deleteByTournamentId(Long tournamentId);
}
