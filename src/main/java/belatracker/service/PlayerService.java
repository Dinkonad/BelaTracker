package belatracker.service;

import belatracker.model.Player;
import belatracker.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public List<Player> getLeaderboard() {
        return playerRepository.findAllByOrderByWinsDescLossesAsc();
    }

    public Player getPlayerById(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Igrač nije pronađen, id: " + id));
    }

    public void savePlayer(Player player) {
        playerRepository.save(player);
    }

    public void deletePlayer(Long id) {
        playerRepository.deleteById(id);
    }

    public List<Player> searchPlayers(String name) {
        return playerRepository.findByNameContainingIgnoreCase(name);
    }
}
