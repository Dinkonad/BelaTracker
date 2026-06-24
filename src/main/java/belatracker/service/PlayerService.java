package belatracker.service;

import belatracker.model.player;
import belatracker.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public player getPlayerById(Long id) {
        return playerRepository.findById(id).orElseThrow();
    }

    public void savePlayer(player player) {
        playerRepository.save(player);
    }

    public void deletePlayer(Long id) {
        playerRepository.deleteById(id);
    }

    public List<player> searchPlayers(String name) {
        return playerRepository.findByNameContainingIgnoreCase(name);
    }
}