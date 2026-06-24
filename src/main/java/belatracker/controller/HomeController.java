package belatracker.controller;

import belatracker.service.PlayerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final PlayerService playerService;

    public HomeController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        model.addAttribute("players", playerService.getLeaderboard());
        return "statistics";
    }

    @GetMapping("/tournaments")
    public String tournaments() {
        return "tournaments";
    }
}
