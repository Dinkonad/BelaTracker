package belatracker.repository;

import belatracker.model.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoundRepository extends JpaRepository<Round, Long> {

    @Query("select coalesce(sum(r.team1Score), 0) from Round r where r.match.id = :matchId")
    int sumTeam1(@Param("matchId") Long matchId);

    @Query("select coalesce(sum(r.team2Score), 0) from Round r where r.match.id = :matchId")
    int sumTeam2(@Param("matchId") Long matchId);
}
