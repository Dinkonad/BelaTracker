package belatracker.controller;

import belatracker.model.Player;
import belatracker.service.PlayerService;
import belatracker.service.StatisticsService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {

    private final PlayerService playerService;
    private final StatisticsService statisticsService;

    public HomeController(PlayerService playerService, StatisticsService statisticsService) {
        this.playerService = playerService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        model.addAttribute("players",     statisticsService.getDetailedPlayerStats());
        model.addAttribute("pairs",       statisticsService.getPairLeaderboard());
        model.addAttribute("h2h",         statisticsService.getHeadToHead());
        model.addAttribute("newPlayer",   new Player());
        return "statistics";
    }

    @PostMapping("/statistics/add-player")
    public String addPlayer(@Valid @ModelAttribute("newPlayer") Player player,
                            BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("players", statisticsService.getDetailedPlayerStats());
            model.addAttribute("pairs",   statisticsService.getPairLeaderboard());
            model.addAttribute("h2h",     statisticsService.getHeadToHead());
            model.addAttribute("showAddForm", true);
            return "statistics";
        }
        playerService.savePlayer(player);
        return "redirect:/statistics?tab=igraci";
    }
}
